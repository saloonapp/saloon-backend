package controllers

import common.FileBodyParser
import models.Exponent
import models.ExponentData
import models.ImportConfig
import services.FileImporter
import services.FileExporter
import infrastructure.repository.common.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Exponents extends Controller {
  val form: Form[ExponentData] = Form(ExponentData.fields)
  val importForm = Form(ImportConfig.fields)
  val repository: Repository[Exponent] = ExponentRepository
  val mainRoute = routes.Exponents
  val viewList = views.html.Application.Exponents.list
  val viewDetails = views.html.Application.Exponents.details
  val viewCreate = views.html.Application.Exponents.create
  val viewUpdate = views.html.Application.Exponents.update
  val viewOps = views.html.Application.Exponents.operations
  def createElt(data: ExponentData): Exponent = ExponentData.toModel(data)
  def toData(elt: Exponent): ExponentData = ExponentData.fromModel(elt)
  def updateElt(elt: Exponent, data: ExponentData): Exponent = ExponentData.merge(elt, data)
  def successCreateFlash(elt: Exponent) = s"Exponent '${elt.name}' has been created"
  def errorCreateFlash(elt: ExponentData) = s"Exponent '${elt.name}' can't be created"
  def successUpdateFlash(elt: Exponent) = s"Exponent '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Exponent) = s"Exponent '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Exponent) = s"Exponent '${elt.name}' has been deleted"
  def successImportFlash(count: Int) = s"${count} exponents imported"

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    for {
      eltPage <- ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, sort.getOrElse("name"))
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage)
        Redirect(mainRoute.list(eventId, query, Some(eltPage.totalPages), sort))
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
                Redirect(mainRoute.details(eventId, uuid)).flashing("success" -> successUpdateFlash(updatedElt))
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

  def operations(eventId: String) = Action.async { implicit req =>
    EventRepository.getByUuid(eventId).map { eventOpt =>
      eventOpt
        .map { event => Ok(viewOps(importForm, event)) }
        .getOrElse(NotFound(views.html.error404()))
    }
  }

  /*def fileImport1(eventId: String) = Action.async(parse.multipartFormData) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        importForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewOps(formWithErrors, event))),
          formData => {
            getFile().map { file =>
              FileImporter.importExponents(file, formData.shouldClean, eventId).map { nbInserted =>
                Redirect(mainRoute.list(eventId)).flashing("success" -> successImportFlash(nbInserted))
              }
            }.getOrElse(Future(BadRequest(viewOps(importForm.fill(formData), event))))
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def fileImport2(eventId: String) = Action.async(parse.tolerantText) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        importForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewOps(formWithErrors, event))),
          formData => {
            val reader = new java.io.StringReader(req.body)
            play.Logger.info("\n\n\nbody: \n\n" + req.body + "\n\n\n")
            play.Logger.info("\n\n\nreader: \n\n" + reader.toString() + "\n\n\n")
            FileImporter.importExponents(reader, formData.shouldClean, eventId).map { nbInserted =>
              Redirect(mainRoute.list(eventId)).flashing("success" -> successImportFlash(nbInserted))
            }
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }*/

  def fileImport(eventId: String) = Action.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        importForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewOps(formWithErrors, event))),
          formData => {
            req.body.file("importedFile").map { filePart =>
              val reader = new java.io.StringReader(new String(filePart.ref))
              FileImporter.importExponents(reader, formData, eventId).map { nbInserted =>
                Redirect(mainRoute.list(eventId)).flashing("success" -> successImportFlash(nbInserted))
              }
            }.getOrElse(Future(BadRequest(viewOps(importForm.fill(formData), event)).flashing("error" -> "You must import a file !")))
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

  /*private def getFile()(implicit req: Request[MultipartFormData[TemporaryFile]]): Option[File] = {
    req.body.file("importedFile").map { data =>
      val file = new File(play.Play.application().path().getAbsolutePath + "/upload/" + System.currentTimeMillis + "_" + data.filename)
      data.ref.moveTo(file)
      file
    }
  }*/
}
