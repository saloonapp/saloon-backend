package admin.controllers

import common.FileBodyParser
import common.models.utils.Page
import common.models.FileImportConfig
import common.models.event.Exponent
import common.models.event.ExponentData
import common.services.FileImporter
import common.services.FileExporter
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.AttendeeRepository
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
  val viewList = admin.views.html.Exponents.list
  val viewDetails = admin.views.html.Exponents.details
  val viewCreate = admin.views.html.Exponents.create
  val viewUpdate = admin.views.html.Exponents.update
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
          .getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def create(eventId: String) = Action.async { implicit req =>
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      eventOpt
        .map { event => Ok(viewCreate(form, allAttendees, event)) }
        .getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doCreate(eventId: String) = Action.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        form.bindFromRequest.fold(
          formWithErrors => AttendeeRepository.findByEvent(eventId).map { allAttendees => BadRequest(viewCreate(formWithErrors, allAttendees, event)) },
          formData => repository.insert(createElt(formData)).flatMap {
            _.map { elt =>
              Future(Redirect(mainRoute.list(eventId)).flashing("success" -> successCreateFlash(elt)))
            }.getOrElse {
              AttendeeRepository.findByEvent(eventId).map { allAttendees =>
                InternalServerError(viewCreate(form.fill(formData), allAttendees, event)).flashing("error" -> errorCreateFlash(formData))
              }
            }
          })
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    val futureData = for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    futureData.flatMap {
      case (eltOpt, eventOpt) =>
        eltOpt.flatMap { elt =>
          eventOpt.map { event =>
            AttendeeRepository.findByUuids(elt.info.team).map { attendees => Ok(viewDetails(elt, attendees, event)) }
          }
        }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def update(eventId: String, uuid: String) = Action.async { implicit req =>
    for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(viewUpdate(form.fill(toData(elt)), elt, allAttendees, event)) }
      }.getOrElse(NotFound(admin.views.html.error404()))
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
            formWithErrors => AttendeeRepository.findByEvent(eventId).map { allAttendees => BadRequest(viewUpdate(formWithErrors, elt, allAttendees, event)) },
            formData => repository.update(uuid, updateElt(elt, formData)).flatMap {
              _.map { updatedElt =>
                Future(Redirect(mainRoute.list(eventId)).flashing("success" -> successUpdateFlash(updatedElt)))
              }.getOrElse {
                AttendeeRepository.findByEvent(eventId).map { allAttendees =>
                  InternalServerError(viewUpdate(form.fill(formData), elt, allAttendees, event)).flashing("error" -> errorUpdateFlash(elt))
                }
              }
            })
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def delete(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list(eventId)).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
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
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
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
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }
}
