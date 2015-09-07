package backend.controllers

import common.models.event.EventId
import common.models.event.Session
import common.models.event.SessionId
import common.models.user.User
import common.services.FileExporter
import common.repositories.event.SessionRepository
import common.repositories.event.AttendeeRepository
import backend.forms.SessionCreateData
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import org.joda.time.DateTime

object Sessions extends SilhouetteEnvironment with ControllerHelpers {
  val createForm: Form[SessionCreateData] = Form(SessionCreateData.fields)

  def list(eventId: EventId, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val curPage = page.getOrElse(1)
    withEvent(eventId) { event =>
      SessionRepository.findByEvent(eventId, query.getOrElse(""), sort.getOrElse("info.start")).map { sessions =>
        val sessionsByDay: List[(DateTime, List[Session])] = sessions.groupBy(_.day()).toList.sortWith(sortByDate)
        Ok(backend.views.html.Events.Sessions.list(sessionsByDay, event))
      }
    }
  }

  def details(eventId: EventId, sessionId: SessionId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withSession(sessionId) { session =>
        AttendeeRepository.findByUuids(session.info.speakers).map { speakers =>
          Ok(backend.views.html.Events.Sessions.details(session, speakers, event))
        }
      }
    }
  }

  def create(eventId: EventId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createView(createForm, eventId)
  }

  def doCreate(eventId: EventId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => createView(formWithErrors, eventId, BadRequest),
      formData => SessionRepository.insert(SessionCreateData.toModel(formData)).flatMap {
        _.map { session =>
          Future(Redirect(backend.controllers.routes.Sessions.details(eventId, session.uuid)).flashing("success" -> s"Session '${session.name}' créée !"))
        }.getOrElse {
          createView(createForm.fill(formData), eventId, InternalServerError)
        }
      })
  }

  def update(eventId: EventId, sessionId: SessionId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withSession(sessionId) { session =>
      updateView(createForm.fill(SessionCreateData.fromModel(session)), session, eventId)
    }
  }

  def doUpdate(eventId: EventId, sessionId: SessionId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withSession(sessionId) { session =>
      createForm.bindFromRequest.fold(
        formWithErrors => updateView(formWithErrors, session, eventId, BadRequest),
        formData => SessionRepository.update(sessionId, SessionCreateData.merge(session, formData)).flatMap {
          _.map { sessionUpdated =>
            Future(Redirect(backend.controllers.routes.Sessions.details(eventId, sessionId)).flashing("success" -> s"La session '${sessionUpdated.name}' a bien été modifiée"))
          }.getOrElse {
            updateView(createForm.fill(formData), session, eventId, InternalServerError)
          }
        })
    }
  }

  def doDelete(eventId: EventId, sessionId: SessionId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withSession(sessionId) { session =>
      // TODO : What to do with linked attendees ?
      //	- delete a session only if it has no speakers ?
      // 	- delete thoses who are not linked with other elts (exponents / sessions)
      //	- ask which one to delete (showing other links)
      SessionRepository.delete(sessionId).map { res =>
        Redirect(backend.controllers.routes.Sessions.list(eventId)).flashing("success" -> s"Suppression de la session '${session.name}'")
      }
    }
  }

  def doFileExport(eventId: EventId) = SecuredAction.async { implicit req =>
    withEvent(eventId) { event =>
      SessionRepository.findByEvent(eventId).map { sessions =>
        val filename = event.name + "_sessions.csv"
        val content = FileExporter.makeCsv(sessions.map(_.toBackendExport))
        Ok(content)
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
          .as("text/csv")
      }
    }
  }

  /*
   * Private methods
   */

  private def sortByDate(e1: (DateTime, List[Session]), e2: (DateTime, List[Session])): Boolean = {
    e1._1.isBefore(e2._1)
  }

  private def createView(createForm: Form[SessionCreateData], eventId: EventId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      for {
        formats <- SessionRepository.findEventFormats(eventId)
        categories <- SessionRepository.findEventCategories(eventId)
        places <- SessionRepository.findEventPlaces(eventId)
      } yield {
        status(backend.views.html.Events.Sessions.create(createForm, formats, categories, places, event))
      }
    }
  }

  private def updateView(createForm: Form[SessionCreateData], session: Session, eventId: EventId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      for {
        formats <- SessionRepository.findEventFormats(eventId)
        categories <- SessionRepository.findEventCategories(eventId)
        places <- SessionRepository.findEventPlaces(eventId)
      } yield {
        status(backend.views.html.Events.Sessions.update(createForm, session, formats, categories, places, event))
      }
    }
  }

}
