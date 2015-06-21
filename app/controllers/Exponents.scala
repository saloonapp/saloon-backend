package controllers

import common.FileBodyParser
import common.models.Page
import models.FileImportConfig
import models.event.Exponent
import models.event.ExponentData
import services.FileImporter
import services.FileExporter
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Exponents extends Controller {
  val form: Form[ExponentData] = Form(ExponentData.fields)
  val fileImportForm = Form(FileImportConfig.fields)
  val repository: Repository[Exponent] = ExponentRepository
  val mainRoute = routes.Exponents
  val viewList = views.html.Application.Exponents.list
  val viewDetails = views.html.Application.Exponents.details
  val viewCreate = views.html.Application.Exponents.create
  val viewUpdate = views.html.Application.Exponents.update
  def createElt(data: ExponentData): Exponent = ExponentData.toModel(data)
  def toData(elt: Exponent): ExponentData = ExponentData.fromModel(elt)
  def updateElt(elt: Exponent, data: ExponentData): Exponent = ExponentData.merge(elt, data)
  def successCreateFlash(elt: Exponent) = s"Exponent '${elt.name}' has been created"
  def errorCreateFlash(elt: ExponentData) = s"Exponent '${elt.name}' can't be created"
  def successUpdateFlash(elt: Exponent) = s"Exponent '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Exponent) = s"Exponent '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Exponent) = s"Exponent '${elt.name}' has been deleted"
  def successImportFlash(count: Int) = s"${count} exponents imported"

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    for {
      eltPage <- ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name"))
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage)
        Redirect(mainRoute.list(eventId, query, Some(eltPage.totalPages), pageSize, sort))
      else
        eventOpt
          .map { event => Ok(viewList(eltPage, event)) }
          .getOrElse(NotFound(views.html.error404()))
    }
  }

  def create(eventId: String) = Action.async { implicit req =>
    EventRepository.getByUuid(eventId).map { eventOpt =>
      eventOpt
        .map { event => Ok(viewCreate(form, event)) }
        .getOrElse(NotFound(views.html.error404()))
    }
  }

  def doCreate(eventId: String) = Action.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        form.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewCreate(formWithErrors, event))),
          formData => repository.insert(createElt(formData)).map {
            _.map { elt =>
              Redirect(mainRoute.list(eventId)).flashing("success" -> successCreateFlash(elt))
            }.getOrElse(InternalServerError(viewCreate(form.fill(formData), event)).flashing("error" -> errorCreateFlash(formData)))
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(viewDetails(elt, event)) }
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  def update(eventId: String, uuid: String) = Action.async { implicit req =>
    for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(viewUpdate(form.fill(toData(elt)), elt, event)) }
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  def doUpdate(eventId: String, uuid: String) = Action.async { implicit req =>
    val dataFuture = for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    dataFuture.flatMap { data =>
      data._1.flatMap { elt =>
        data._2.map { event =>
          form.bindFromRequest.fold(
            formWithErrors => Future(BadRequest(viewUpdate(formWithErrors, elt, event))),
            formData => repository.update(uuid, updateElt(elt, formData)).map {
              _.map { updatedElt =>
                Redirect(mainRoute.list(eventId)).flashing("success" -> successUpdateFlash(updatedElt))
              }.getOrElse(InternalServerError(viewUpdate(form.fill(formData), elt, event)).flashing("error" -> errorUpdateFlash(elt)))
            })
        }
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def delete(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list(eventId)).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  // TODO : add preview of updates
  def fileImport(eventId: String) = Action.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        fileImportForm.bindFromRequest.fold(
          formWithErrors => Future(Redirect(routes.Events.operations(eventId)).flashing("error" -> "Form error...")),
          formData => {
            req.body.file("importedFile").map { filePart =>
              val reader = new java.io.StringReader(new String(filePart.ref, formData.encoding))
              FileImporter.importExponents(reader, formData, eventId).map {
                case (nbInserted, errors) =>
                  Redirect(routes.Events.details(eventId))
                    .flashing(
                      "success" -> successImportFlash(nbInserted),
                      "error" -> (if (errors.isEmpty) { "" } else { "Errors: <br>" + errors.map("- " + _.toString).mkString("<br>") }))
              }
            }.getOrElse(Future(Redirect(routes.Events.operations(eventId)).flashing("error" -> "You must import a file !")))
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def fileExport(eventId: String) = Action.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        ExponentRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_exponents.csv"
          val content = FileExporter.makeCsv(elts.map(_.toMap))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }
}
