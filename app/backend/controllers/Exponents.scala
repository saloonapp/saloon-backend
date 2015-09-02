package backend.controllers

import common.models.event.Attendee
import common.models.event.Exponent
import common.models.user.User
import common.models.utils.Page
import common.services.FileExporter
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import backend.forms.ExponentCreateData
import backend.forms.AttendeeCreateData
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

object Exponents extends SilhouetteEnvironment with ControllerHelpers {
  val createForm: Form[ExponentCreateData] = Form(ExponentCreateData.fields)

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val curPage = page.getOrElse(1)
    withEvent(eventId) { event =>
      ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name")).map { exponentPage =>
        if (1 < curPage && exponentPage.totalPages < curPage) {
          Redirect(backend.controllers.routes.Exponents.list(eventId, query, Some(exponentPage.totalPages), pageSize, sort))
        } else {
          Ok(backend.views.html.Events.Exponents.list(exponentPage, event))
        }
      }
    }
  }

  def details(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withExponent(exponentId) { exponent =>
        AttendeeRepository.findByUuids(exponent.info.team).map { team =>
          Ok(backend.views.html.Events.Exponents.details(exponent, team, event))
        }
      }
    }
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createView(createForm, eventId)
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => createView(formWithErrors, eventId, BadRequest),
      formData => ExponentRepository.insert(ExponentCreateData.toModel(formData)).flatMap {
        _.map { exponent =>
          Future(Redirect(backend.controllers.routes.Exponents.details(eventId, exponent.uuid)).flashing("success" -> s"Exposant '${exponent.name}' créé !"))
        }.getOrElse {
          createView(createForm.fill(formData), eventId, InternalServerError)
        }
      })
  }

  def update(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withExponent(exponentId) { exponent =>
      updateView(createForm.fill(ExponentCreateData.fromModel(exponent)), exponent, eventId)
    }
  }

  def doUpdate(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withExponent(exponentId) { exponent =>
        createForm.bindFromRequest.fold(
          formWithErrors => updateView(formWithErrors, exponent, eventId, BadRequest),
          formData => ExponentRepository.update(exponentId, ExponentCreateData.merge(exponent, formData)).flatMap {
            _.map { exponentUpdated =>
              Future(Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> s"L'exposant '${exponentUpdated.name}' a bien été modifié"))
            }.getOrElse {
              updateView(createForm.fill(formData), exponent, eventId, InternalServerError)
            }
          })
      }
    }
  }

  def doDelete(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withExponent(exponentId) { exponent =>
      // TODO : What to do with linked attendees ?
      // 	- delete thoses who are not linked with other elts (exponents / sessions)
      //	- ask which one to delete (showing other links)
      ExponentRepository.delete(exponentId).map { res =>
        Redirect(backend.controllers.routes.Exponents.list(eventId)).flashing("success" -> s"Suppression de l'exposant '${exponent.name}'")
      }
    }
  }

  def doFileExport(eventId: String) = SecuredAction.async { implicit req =>
    withEvent(eventId) { event =>
      ExponentRepository.findByEvent(eventId).map { exponents =>
        val filename = event.name + "_exponents.csv"
        val content = FileExporter.makeCsv(exponents.map(_.toBackendExport))
        Ok(content)
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
          .as("text/csv")
      }
    }
  }

  /*
   * Team
   */

  val teamCreateForm: Form[AttendeeCreateData] = Form(AttendeeCreateData.fields)
  val teamJoinForm: Form[String] = Form(single("attendeeId" -> nonEmptyText))

  def teamDetails(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withAttendee(attendeeId) { attendee =>
        for {
          exponentOpt <- ExponentRepository.getByUuid(exponentId)
          attendeeSessions <- SessionRepository.findByEventAttendee(eventId, attendeeId)
          attendeeExponents <- ExponentRepository.findByEventAttendee(eventId, attendeeId)
        } yield {
          if (exponentOpt.isDefined && exponentOpt.get.hasMember(attendee)) {
            Ok(backend.views.html.Events.Exponents.Team.details(attendee, attendeeSessions, attendeeExponents, event, exponentOpt.get))
          } else {
            Ok(backend.views.html.Events.Attendees.details(attendee, attendeeSessions, attendeeExponents, event))
          }
        }
      }
    }
  }

  def teamCreate(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    teamCreateView(teamCreateForm, teamJoinForm, eventId, exponentId)
  }

  def doTeamCreateInvite(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    teamCreateForm.bindFromRequest.fold(
      formWithErrors => teamCreateView(formWithErrors, teamJoinForm, eventId, exponentId, "inviteuser", BadRequest),
      formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
        _.map { attendee =>
          ExponentRepository.addTeamMember(exponentId, attendee.uuid).map { r =>
            // TODO : create ExponentInvite & send invite email
            Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> "Nouveau membre invité !")
          }
        }.getOrElse {
          teamCreateView(teamCreateForm.fill(formData), teamJoinForm, eventId, exponentId, "inviteuser", InternalServerError)
        }
      })
  }

  def doTeamCreateFull(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    teamCreateForm.bindFromRequest.fold(
      formWithErrors => teamCreateView(formWithErrors, teamJoinForm, eventId, exponentId, "fullform", BadRequest),
      formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
        _.map { attendee =>
          ExponentRepository.addTeamMember(exponentId, attendee.uuid).map { r =>
            Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> "Nouveau membre créé !")
          }
        }.getOrElse {
          teamCreateView(teamCreateForm.fill(formData), teamJoinForm, eventId, exponentId, "fullform", InternalServerError)
        }
      })
  }

  def doTeamJoin(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    teamJoinForm.bindFromRequest.fold(
      formWithErrors => teamCreateView(teamCreateForm, formWithErrors, eventId, exponentId, "fromattendees", BadRequest),
      formData => ExponentRepository.addTeamMember(exponentId, formData).flatMap { err =>
        if (err.ok) {
          Future(Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> "Nouveau membre ajouté !"))
        } else {
          teamCreateView(teamCreateForm, teamJoinForm.fill(formData), eventId, exponentId, "fromattendees", InternalServerError)
        }
      })
  }

  def teamUpdate(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(eventId) { attendee =>
      teamUpdateView(teamCreateForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, eventId, exponentId)
    }
  }

  def doTeamUpdate(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(eventId) { attendee =>
      teamCreateForm.bindFromRequest.fold(
        formWithErrors => teamUpdateView(formWithErrors, attendee, eventId, exponentId, BadRequest),
        formData => AttendeeRepository.update(attendeeId, AttendeeCreateData.merge(attendee, formData)).flatMap {
          _.map { attendeeUpdated =>
            Future(Redirect(backend.controllers.routes.Exponents.teamDetails(eventId, exponentId, attendeeId)).flashing("success" -> s"${attendeeUpdated.name} a été modifié"))
          }.getOrElse {
            teamUpdateView(teamCreateForm.fill(formData), attendee, eventId, exponentId, InternalServerError)
          }
        })
    }
  }

  def doTeamLeave(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    ExponentRepository.removeTeamMember(exponentId, attendeeId).map { r =>
      Redirect(req.headers("referer"))
    }
  }

  // def doTeamInvite(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
  // def doTeamBan(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>

  /*
   * Private methods
   */

  private def createView(createForm: Form[ExponentCreateData], eventId: String, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      Future(status(backend.views.html.Events.Exponents.create(createForm, event)))
    }
  }

  private def updateView(createForm: Form[ExponentCreateData], exponent: Exponent, eventId: String, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      Future(status(backend.views.html.Events.Exponents.update(createForm.fill(ExponentCreateData.fromModel(exponent)), exponent, event)))
    }
  }

  private def teamCreateView(teamCreateForm: Form[AttendeeCreateData], teamJoinForm: Form[String], eventId: String, exponentId: String, tab: String = "inviteuser", status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      withExponent(exponentId) { exponent =>
        AttendeeRepository.findByEvent(eventId).map { allAttendees =>
          status(backend.views.html.Events.Exponents.Team.create(teamCreateForm, teamJoinForm, allAttendees.filter(!exponent.hasMember(_)), event, exponent, tab))
        }
      }
    }
  }

  private def teamUpdateView(teamCreateForm: Form[AttendeeCreateData], attendee: Attendee, eventId: String, exponentId: String, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      withExponent(exponentId) { exponent =>
        Future(status(backend.views.html.Events.Exponents.Team.update(teamCreateForm, attendee, event, exponent)))
      }
    }
  }

}
