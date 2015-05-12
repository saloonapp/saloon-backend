package controllers

import common.FileBodyParser
import models.Session
import models.SessionData
import models.ImportConfig
import services.FileImporter
import services.FileExporter
import infrastructure.repository.common.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Sessions extends Controller {
  val form: Form[SessionData] = Form(SessionData.fields)
  val importForm = Form(ImportConfig.fields)
  val repository: Repository[Session] = SessionRepository
  val mainRoute = routes.Sessions
  val viewList = views.html.Application.Sessions.list
  val viewDetails = views.html.Application.Sessions.details
  val viewCreate = views.html.Application.Sessions.create
  val viewUpdate = views.html.Application.Sessions.update
  val viewOps = views.html.Application.Sessions.operations
  def createElt(data: SessionData): Session = SessionData.toModel(data)
  def toData(elt: Session): SessionData = SessionData.fromModel(elt)
  def updateElt(elt: Session, data: SessionData): Session = SessionData.merge(elt, data)
  def successCreateFlash(elt: Session) = s"Exponent '${elt.title}' has been created"
  def errorCreateFlash(elt: SessionData) = s"Exponent '${elt.title}' can't be created"
  def successUpdateFlash(elt: Session) = s"Exponent '${elt.title}' has been modified"
  def errorUpdateFlash(elt: Session) = s"Exponent '${elt.title}' can't be modified"
  def successDeleteFlash(elt: Session) = s"Exponent '${elt.title}' has been deleted"
  def successImportFlash(count: Int) = s"${count} exponents imported"

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    for {
      eltPage <- SessionRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, sort.getOrElse("-start"))
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

  def fileImport(eventId: String) = Action.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        importForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewOps(formWithErrors, event))),
          formData => {
            req.body.file("importedFile").map { filePart =>
              val reader = new java.io.StringReader(new String(filePart.ref))
              FileImporter.importSessions(reader, formData, eventId).map {
                case (nbInserted, errors) =>
                  Redirect(mainRoute.list(eventId))
                    .flashing(
                      "success" -> successImportFlash(nbInserted),
                      "error" -> (if (errors.isEmpty) { "" } else { "Errors: <br>" + errors.map("- " + _.toString).mkString("<br>") }))
              }
            }.getOrElse(Future(BadRequest(viewOps(importForm.fill(formData), event)).flashing("error" -> "You must import a file !")))
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def fileExport(eventId: String) = Action.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        SessionRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_sessions.csv"
          val content = FileExporter.makeCsv(elts.map(_.toMap))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }
}
