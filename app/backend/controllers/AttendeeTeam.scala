package backend.controllers

import common.models.values.typed.ItemType
import common.models.values.typed.GenericId
import common.models.values.UUID
import common.models.event.EventId
import common.models.event.Attendee
import common.models.event.AttendeeId
import common.models.event.Exponent
import common.models.event.ExponentId
import common.models.event.Session
import common.models.event.SessionId
import common.models.user.User
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import common.repositories.event.EventItemRepository
import backend.forms.AttendeeCreateData
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

object AttendeeTeam extends SilhouetteEnvironment with ControllerHelpers {
  val createForm: Form[AttendeeCreateData] = Form(AttendeeCreateData.fields)
  val joinForm: Form[AttendeeId] = Form(single("attendeeId" -> of[AttendeeId]))

  def details(eventId: EventId, itemType: ItemType, genericId: GenericId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withAttendee(attendeeId) { attendee =>
        for {
          itemOpt <- EventItemRepository.getByUuid(itemType, genericId)
          attendeeSessions <- SessionRepository.findByEventAttendee(eventId, attendeeId)
          attendeeExponents <- ExponentRepository.findByEventAttendee(eventId, attendeeId)
        } yield {
          if (itemOpt.flatMap(_.toExponent).map(_.hasMember(attendee)).getOrElse(false)) {
            Ok(backend.views.html.Events.AttendeeTeam.details(attendee, attendeeSessions, attendeeExponents, event, itemOpt.get))
          } else if (itemOpt.flatMap(_.toSession).map(_.hasMember(attendee)).getOrElse(false)) {
            Ok(backend.views.html.Events.AttendeeTeam.details(attendee, attendeeSessions, attendeeExponents, event, itemOpt.get))
          } else {
            Ok(backend.views.html.Events.Attendees.details(attendee, attendeeSessions, attendeeExponents, event))
          }
        }
      }
    }
  }

  def create(eventId: EventId, itemType: ItemType, genericId: GenericId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createView(createForm, joinForm, eventId, itemType, genericId)
  }

  def doCreateInvite(eventId: EventId, itemType: ItemType, genericId: GenericId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => createView(formWithErrors, joinForm, eventId, itemType, genericId, "inviteuser", BadRequest),
      formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
        _.map { attendee =>
          itemType match {
            case ItemType.exponents => ExponentRepository.addTeamMember(genericId.toExponentId, attendee.uuid).map { r =>
              // TODO : create ExponentInvite & send invite email
              Redirect(backend.controllers.routes.Exponents.details(eventId, genericId.toExponentId)).flashing("success" -> "Nouveau membre invité !")
            }
            case ItemType.sessions => SessionRepository.addSpeaker(genericId.toSessionId, attendee.uuid).map { r =>
              // TODO : create ExponentInvite & send invite email
              Redirect(backend.controllers.routes.Sessions.details(eventId, genericId.toSessionId)).flashing("success" -> "Nouveau speaker invité !")
            }
            case _ => Future(NotFound(backend.views.html.error("403", s"Unknown ItemType $itemType")))
          }
        }.getOrElse {
          createView(createForm.fill(formData), joinForm, eventId, itemType, genericId, "inviteuser", InternalServerError)
        }
      })
  }

  def doCreateFull(eventId: EventId, itemType: ItemType, genericId: GenericId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => createView(formWithErrors, joinForm, eventId, itemType, genericId, "fullform", BadRequest),
      formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
        _.map { attendee =>
          itemType match {
            case ItemType.exponents => ExponentRepository.addTeamMember(genericId.toExponentId, attendee.uuid).map { r =>
              Redirect(backend.controllers.routes.Exponents.details(eventId, genericId.toExponentId)).flashing("success" -> "Nouveau membre créé !")
            }
            case ItemType.sessions => SessionRepository.addSpeaker(genericId.toSessionId, attendee.uuid).map { r =>
              Redirect(backend.controllers.routes.Sessions.details(eventId, genericId.toSessionId)).flashing("success" -> "Nouveau speaker créé !")
            }
            case _ => Future(NotFound(backend.views.html.error("403", s"Unknown ItemType $itemType")))
          }
        }.getOrElse {
          createView(createForm.fill(formData), joinForm, eventId, itemType, genericId, "fullform", InternalServerError)
        }
      })
  }

  def doJoin(eventId: EventId, itemType: ItemType, genericId: GenericId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    joinForm.bindFromRequest.fold(
      formWithErrors => createView(createForm, formWithErrors, eventId, itemType, genericId, "fromattendees", BadRequest),
      formData => itemType match {
        case ItemType.exponents => ExponentRepository.addTeamMember(genericId.toExponentId, formData).map { r =>
          Redirect(backend.controllers.routes.Exponents.details(eventId, genericId.toExponentId)).flashing("success" -> "Nouveau membre ajouté !")
        }
        case ItemType.sessions => SessionRepository.addSpeaker(genericId.toSessionId, formData).map { r =>
          Redirect(backend.controllers.routes.Sessions.details(eventId, genericId.toSessionId)).flashing("success" -> "Nouveau speaker ajouté !")
        }
        case _ => Future(NotFound(backend.views.html.error("403", s"Unknown ItemType $itemType")))
      })
  }

  def update(eventId: EventId, itemType: ItemType, genericId: GenericId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(attendeeId) { attendee =>
      updateView(createForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, eventId, itemType, genericId)
    }
  }

  def doUpdate(eventId: EventId, itemType: ItemType, genericId: GenericId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(attendeeId) { attendee =>
      createForm.bindFromRequest.fold(
        formWithErrors => updateView(formWithErrors, attendee, eventId, itemType, genericId, BadRequest),
        formData => AttendeeRepository.update(attendeeId, AttendeeCreateData.merge(attendee, formData)).flatMap {
          _.map { attendeeUpdated =>
            Future(Redirect(backend.controllers.routes.AttendeeTeam.details(eventId, itemType, genericId, attendeeId)).flashing("success" -> s"${attendeeUpdated.name} a été modifié"))
          }.getOrElse {
            updateView(createForm.fill(formData), attendee, eventId, itemType, genericId, InternalServerError)
          }
        })
    }
  }

  def doLeave(eventId: EventId, itemType: ItemType, genericId: GenericId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    itemType match {
      case ItemType.exponents => ExponentRepository.removeTeamMember(genericId.toExponentId, attendeeId).map { r =>
        Redirect(req.headers("referer"))
      }
      case ItemType.sessions => SessionRepository.removeSpeaker(genericId.toSessionId, attendeeId).map { r =>
        Redirect(req.headers("referer"))
      }
      case _ => Future(NotFound(backend.views.html.error("403", s"Unknown ItemType $itemType")))
    }
  }

  // def doTeamInvite(eventId: EventId, itemType: ItemType, genericId: GenericId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
  // def doTeamBan(eventId: EventId, itemType: ItemType, genericId: GenericId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>

  /*
   * Private methods
   */

  private def createView(createForm: Form[AttendeeCreateData], joinForm: Form[AttendeeId], eventId: EventId, itemType: ItemType, genericId: GenericId, tab: String = "inviteuser", status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      withEventItem(itemType, genericId) { eventItem =>
        AttendeeRepository.findByEvent(eventId).map { allAttendees =>
          val notInTeamAttendees = allAttendees.filter(a => !eventItem.toExponent.map(_.hasMember(a)).orElse(eventItem.toSession.map(_.hasMember(a))).getOrElse(false))
          status(backend.views.html.Events.AttendeeTeam.create(createForm, joinForm, notInTeamAttendees, event, eventItem, tab))
        }
      }
    }
  }

  private def updateView(createForm: Form[AttendeeCreateData], attendee: Attendee, eventId: EventId, itemType: ItemType, genericId: GenericId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      withEventItem(itemType, genericId) { eventItem =>
        Future(status(backend.views.html.Events.AttendeeTeam.update(createForm, attendee, event, eventItem)))
      }
    }
  }
}