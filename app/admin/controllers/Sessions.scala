package admin.controllers

import common.FileBodyParser
import common.models.utils.Page
import common.models.FileImportConfig
import common.models.event.EventId
import common.models.event.Session
import common.models.event.SessionId
import common.models.event.SessionData
import common.services.FileImporter
import common.services.FileExporter
import common.repositories.Repository
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Sessions extends SilhouetteEnvironment {
  val form: Form[SessionData] = Form(SessionData.fields)
  val fileImportForm = Form(FileImportConfig.fields)
  val repository: Repository[Session, SessionId] = SessionRepository
  val mainRoute = routes.Sessions
  val viewList = admin.views.html.Sessions.list
  val viewDetails = admin.views.html.Sessions.details
  val viewCreate = admin.views.html.Sessions.create
  val viewUpdate = admin.views.html.Sessions.update
  def createElt(data: SessionData): Session = SessionData.toModel(data)
  def toData(elt: Session): SessionData = SessionData.fromModel(elt)
  def updateElt(elt: Session, data: SessionData): Session = SessionData.merge(elt, data)
  def successCreateFlash(elt: Session) = s"Session '${elt.name}' has been created"
  def errorCreateFlash(elt: SessionData) = s"Session '${elt.name}' can't be created"
  def successUpdateFlash(elt: Session) = s"Session '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Session) = s"Session '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Session) = s"Session '${elt.name}' has been deleted"
  def successImportFlash(count: Int) = s"${count} sessions imported"

  def list(eventId: EventId, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    val curPage = page.getOrElse(1)
    for {
      eltPage <- SessionRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("-start"))
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

  def create(eventId: EventId) = SecuredAction.async { implicit req =>
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      eventOpt
        .map { event => Ok(viewCreate(form, allAttendees, event)) }
        .getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doCreate(eventId: EventId) = SecuredAction.async { implicit req =>
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

  def details(eventId: EventId, uuid: SessionId) = SecuredAction.async { implicit req =>
    val futureData = for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    futureData.flatMap {
      case (eltOpt, eventOpt) =>
        eltOpt.flatMap { elt =>
          eventOpt.map { event =>
            AttendeeRepository.findByUuids(elt.info.speakers).map { attendees => Ok(viewDetails(elt, attendees, event)) }
          }
        }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def update(eventId: EventId, uuid: SessionId) = SecuredAction.async { implicit req =>
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

  def doUpdate(eventId: EventId, uuid: SessionId) = SecuredAction.async { implicit req =>
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

  def delete(eventId: EventId, uuid: SessionId) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list(eventId)).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  // TODO : add preview of updates
  /*def fileImport(eventId: EventId) = SecuredAction.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        fileImportForm.bindFromRequest.fold(
          formWithErrors => Future(Redirect(routes.Events.operations(eventId)).flashing("error" -> "Form error...")),
          formData => {
            req.body.file("importedFile").map { filePart =>
              val reader = new java.io.StringReader(new String(filePart.ref, formData.encoding))
              FileImporter.importSessions(reader, formData, eventId).map {
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

  def fileExport(eventId: EventId) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        SessionRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_sessions.csv"
          val content = FileExporter.makeCsv(elts.map(_.toMap))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }*/
}
