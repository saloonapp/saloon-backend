package backend.controllers

import common.models.event.Session
import common.models.user.User
import common.models.user.UserInfo
import common.services.FileExporter
import common.repositories.event.EventRepository
import common.repositories.event.SessionRepository
import common.repositories.event.AttendeeRepository
import backend.forms.SessionCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import org.joda.time.DateTime
import com.mohiva.play.silhouette.core.LoginInfo

object Sessions extends SilhouetteEnvironment {
  val createForm: Form[SessionCreateData] = Form(SessionCreateData.fields)

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val curPage = page.getOrElse(1)
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      elts <- SessionRepository.findByEvent(eventId, query.getOrElse(""), sort.getOrElse("info.start"))
    } yield {
      eventOpt
        .map { event =>
          val sessionsByDay: List[(DateTime, List[Session])] = elts.groupBy(startDate).toList.sortWith(sortByDate)
          Ok(backend.views.html.Events.Sessions.list(sessionsByDay, event))
        }.getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val futureData = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- SessionRepository.getByUuid(uuid)
    } yield (eltOpt, eventOpt)
    futureData.flatMap {
      case (eltOpt, eventOpt) =>
        eltOpt.flatMap { elt =>
          eventOpt.map { event =>
            AttendeeRepository.findByUuids(elt.info.speakers).map { speakers => Ok(backend.views.html.Events.Sessions.details(elt, speakers, event)) }
          }
        }.getOrElse { Future(NotFound(backend.views.html.error("404", "Event not found..."))) }
    }
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
      formats <- SessionRepository.findEventFormats(eventId)
      categories <- SessionRepository.findEventCategories(eventId)
      places <- SessionRepository.findEventPlaces(eventId)
    } yield {
      eventOpt
        .map { event => Ok(backend.views.html.Events.Sessions.create(createForm, allAttendees, formats, categories, places, event)) }
        .getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        createForm.bindFromRequest.fold(
          formWithErrors => for {
            allAttendees <- AttendeeRepository.findByEvent(eventId)
            formats <- SessionRepository.findEventFormats(eventId)
            categories <- SessionRepository.findEventCategories(eventId)
            places <- SessionRepository.findEventPlaces(eventId)
          } yield BadRequest(backend.views.html.Events.Sessions.create(formWithErrors, allAttendees, formats, categories, places, event)),
          formData => SessionRepository.insert(SessionCreateData.toModel(formData)).flatMap {
            _.map { elt =>
              Future(Redirect(backend.controllers.routes.Sessions.details(eventId, elt.uuid)).flashing("success" -> s"Session '${elt.name}' créée !"))
            }.getOrElse {
              for {
                allAttendees <- AttendeeRepository.findByEvent(eventId)
                formats <- SessionRepository.findEventFormats(eventId)
                categories <- SessionRepository.findEventCategories(eventId)
                places <- SessionRepository.findEventPlaces(eventId)
              } yield InternalServerError(backend.views.html.Events.Sessions.create(createForm.fill(formData), allAttendees, formats, categories, places, event)).flashing("error" -> s"Impossible de créer la session '${formData.name}'")
            }
          })
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def update(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    for {
      eltOpt <- SessionRepository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
      formats <- SessionRepository.findEventFormats(eventId)
      categories <- SessionRepository.findEventCategories(eventId)
      places <- SessionRepository.findEventPlaces(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Events.Sessions.update(createForm.fill(SessionCreateData.fromModel(elt)), elt, allAttendees, formats, categories, places, event)) }
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doUpdate(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val dataFuture = for {
      eltOpt <- SessionRepository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    dataFuture.flatMap { data =>
      data._1.flatMap { elt =>
        data._2.map { event =>
          createForm.bindFromRequest.fold(
            formWithErrors => for {
              allAttendees <- AttendeeRepository.findByEvent(eventId)
              formats <- SessionRepository.findEventFormats(eventId)
              categories <- SessionRepository.findEventCategories(eventId)
              places <- SessionRepository.findEventPlaces(eventId)
            } yield BadRequest(backend.views.html.Events.Sessions.update(formWithErrors, elt, allAttendees, formats, categories, places, event)),
            formData => SessionRepository.update(uuid, SessionCreateData.merge(elt, formData)).flatMap {
              _.map { updatedElt =>
                Future(Redirect(backend.controllers.routes.Sessions.details(eventId, updatedElt.uuid)).flashing("success" -> s"La session '${updatedElt.name}' a bien été modifiée"))
              }.getOrElse {
                for {
                  allAttendees <- AttendeeRepository.findByEvent(eventId)
                  formats <- SessionRepository.findEventFormats(eventId)
                  categories <- SessionRepository.findEventCategories(eventId)
                  places <- SessionRepository.findEventPlaces(eventId)
                } yield InternalServerError(backend.views.html.Events.Sessions.update(createForm.fill(formData), elt, allAttendees, formats, categories, places, event)).flashing("error" -> s"Impossible de modifier la session '${elt.name}'")
              }
            })
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def delete(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    SessionRepository.getByUuid(uuid).map {
      _.map { elt =>
        SessionRepository.delete(uuid)
        Redirect(backend.controllers.routes.Sessions.list(eventId)).flashing("success" -> s"Suppression de la session '${elt.name}'")
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def fileExport(eventId: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        SessionRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_sessions.csv"
          val content = FileExporter.makeCsv(elts.map(_.toBackendExport))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  private def startDate(s: Session): DateTime = {
    if (s.info.start.isEmpty || s.info.end.isEmpty) new DateTime(0)
    else s.info.start.get.withTimeAtStartOfDay()
  }
  private def sortByDate(e1: (DateTime, List[Session]), e2: (DateTime, List[Session])): Boolean = {
    e1._1.isBefore(e2._1)
  }

}
