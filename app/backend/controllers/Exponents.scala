package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.utils.Page
import common.services.FileExporter
import common.repositories.event.EventRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import common.repositories.event.AttendeeRepository
import backend.forms.ExponentCreateData
import backend.forms.AttendeeCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.core.LoginInfo

object Exponents extends SilhouetteEnvironment {
  val createForm: Form[ExponentCreateData] = Form(ExponentCreateData.fields)

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val curPage = page.getOrElse(1)
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltPage <- ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name"))
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage) {
        Redirect(backend.controllers.routes.Exponents.list(eventId, query, Some(eltPage.totalPages), pageSize, sort))
      } else {
        eventOpt
          .map { event => Ok(backend.views.html.Events.Exponents.list(eltPage, event)) }
          .getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
      }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val res: Future[Future[Result]] = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- ExponentRepository.getByUuid(uuid)
    } yield {
      val res2: Option[Future[Result]] = for {
        event <- eventOpt
        elt <- eltOpt
      } yield {
        AttendeeRepository.findByUuids(elt.info.team).map { team =>
          Ok(backend.views.html.Events.Exponents.details(elt, team, event))
        }
      }
      res2.getOrElse { Future(NotFound(backend.views.html.error("404", "Event not found..."))) }
    }
    res.flatMap(identity)
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eventOpt
        .map { event => Ok(backend.views.html.Events.Exponents.create(createForm, event)) }
        .getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        createForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Events.Exponents.create(formWithErrors, event))),
          formData => ExponentRepository.insert(ExponentCreateData.toModel(formData)).map {
            _.map { elt =>
              Redirect(backend.controllers.routes.Exponents.details(eventId, elt.uuid)).flashing("success" -> s"Exposant '${elt.name}' créé !")
            }.getOrElse {
              InternalServerError(backend.views.html.Events.Exponents.create(createForm.fill(formData), event)).flashing("error" -> s"Impossible de créer l'exposant '${formData.name}'")
            }
          })
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def update(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eltOpt <- ExponentRepository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Events.Exponents.update(createForm.fill(ExponentCreateData.fromModel(elt)), elt, event)) }
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doUpdate(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val dataFuture = for {
      eltOpt <- ExponentRepository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    dataFuture.flatMap { data =>
      data._1.flatMap { elt =>
        data._2.map { event =>
          createForm.bindFromRequest.fold(
            formWithErrors => Future(BadRequest(backend.views.html.Events.Exponents.update(formWithErrors, elt, event))),
            formData => ExponentRepository.update(uuid, ExponentCreateData.merge(elt, formData)).map {
              _.map { updatedElt =>
                Redirect(backend.controllers.routes.Exponents.details(eventId, updatedElt.uuid)).flashing("success" -> s"L'exposant '${updatedElt.name}' a bien été modifié")
              }.getOrElse {
                InternalServerError(backend.views.html.Events.Exponents.update(createForm.fill(formData), elt, event)).flashing("error" -> s"Impossible de modifier l'exposant '${elt.name}'")
              }
            })
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def delete(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    ExponentRepository.getByUuid(uuid).map {
      _.map { elt =>
        ExponentRepository.delete(uuid)
        // TODO : What to do with linked attendees ?
        // 	- delete thoses who are not linked with other elts (exponents / sessions)
        //	- ask which one to delete (showing other links)
        Redirect(backend.controllers.routes.Exponents.list(eventId)).flashing("success" -> s"Suppression de l'exposant '${elt.name}'")
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def fileExport(eventId: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        ExponentRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_exponents.csv"
          val content = FileExporter.makeCsv(elts.map(_.toBackendExport))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  /*
   *
   * Team
   *
   */

  val teamCreateForm: Form[AttendeeCreateData] = Form(AttendeeCreateData.fields)
  val teamJoinForm = Form(single("attendeeId" -> nonEmptyText))

  def teamCreate(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      val res: Option[Result] = for {
        event <- eventOpt
        exponent <- exponentOpt
      } yield {
        Ok(backend.views.html.Events.Exponents.Team.create(teamCreateForm, teamJoinForm, allAttendees.filter(!exponent.hasMember(_)), event, exponent))
      }
      res.getOrElse(NotFound(backend.views.html.error("404", "Not found...")))
    }
  }

  def doTeamCreateFull(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val res: Future[Future[Result]] = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      val res2: Option[Future[Result]] = for {
        event <- eventOpt
        exponent <- exponentOpt
      } yield {
        teamCreateForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Events.Exponents.Team.create(formWithErrors, teamJoinForm, allAttendees.filter(!exponent.hasMember(_)), event, exponent, "fullform"))),
          formData => {
            val attendee = AttendeeCreateData.toModel(formData)
            AttendeeRepository.insert(attendee).flatMap {
              _.map { elt =>
                ExponentRepository.addTeamMember(exponentId, attendee.uuid).map { r =>
                  Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> "Nouveau membre créé !")
                }
              }.getOrElse {
                Future(InternalServerError(backend.views.html.Events.Exponents.Team.create(teamCreateForm.fill(formData), teamJoinForm, allAttendees.filter(!exponent.hasMember(_)), event, exponent, "fullform")))
              }
            }
          })
      }
      res2.getOrElse(Future(NotFound(backend.views.html.error("404", "Not found..."))))
    }
    res.flatMap(identity)
  }

  def doTeamCreateInvite(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val res: Future[Future[Result]] = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      val res2: Option[Future[Result]] = for {
        event <- eventOpt
        exponent <- exponentOpt
      } yield {
        teamCreateForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Events.Exponents.Team.create(formWithErrors, teamJoinForm, allAttendees.filter(!exponent.hasMember(_)), event, exponent, "inviteuser"))),
          formData => {
            val attendee = AttendeeCreateData.toModel(formData)
            AttendeeRepository.insert(attendee).flatMap {
              _.map { elt =>
                ExponentRepository.addTeamMember(exponentId, attendee.uuid).map { r =>
                  // TODO : create ExponentInvite & send invite email
                  Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> "Nouveau membre invité !")
                }
              }.getOrElse {
                Future(InternalServerError(backend.views.html.Events.Exponents.Team.create(teamCreateForm.fill(formData), teamJoinForm, allAttendees.filter(!exponent.hasMember(_)), event, exponent, "inviteuser")))
              }
            }
          })
      }
      res2.getOrElse(Future(NotFound(backend.views.html.error("404", "Not found..."))))
    }
    res.flatMap(identity)
  }

  def doTeamJoin(eventId: String, exponentId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val res: Future[Future[Result]] = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      val res2: Option[Future[Result]] = for {
        event <- eventOpt
        exponent <- exponentOpt
      } yield {
        teamJoinForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Events.Exponents.Team.create(teamCreateForm, formWithErrors, allAttendees.filter(!exponent.hasMember(_)), event, exponent, "fromattendees"))),
          formData => ExponentRepository.addTeamMember(exponentId, formData).map { r =>
            Redirect(backend.controllers.routes.Exponents.details(eventId, exponentId)).flashing("success" -> "Nouveau membre ajouté !")
          })
      }
      res2.getOrElse(Future(NotFound(backend.views.html.error("404", "Not found..."))))
    }
    res.flatMap(identity)
  }

  def teamDetails(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      attendeeOpt <- AttendeeRepository.getByUuid(attendeeId)
      attendeeSessions <- SessionRepository.findByEventAttendee(eventId, attendeeId)
      attendeeExponents <- ExponentRepository.findByEventAttendee(eventId, attendeeId)
    } yield {
      val res: Option[Result] = for {
        event <- eventOpt
        exponent <- exponentOpt
        attendee <- attendeeOpt
      } yield {
        if (exponent.hasMember(attendee)) {
          Ok(backend.views.html.Events.Exponents.Team.details(attendee, attendeeSessions, attendeeExponents, event, exponent))
        } else {
          Ok(backend.views.html.Events.Attendees.details(attendee, attendeeSessions, attendeeExponents, event))
        }
      }
      res.getOrElse(NotFound(backend.views.html.error("404", "Not found...")))
    }
  }

  def teamUpdate(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      attendeeOpt <- AttendeeRepository.getByUuid(attendeeId)
    } yield {
      val res: Option[Result] = for {
        event <- eventOpt
        exponent <- exponentOpt
        attendee <- attendeeOpt
      } yield {
        Ok(backend.views.html.Events.Exponents.Team.update(teamCreateForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, event, exponent))
      }
      res.getOrElse(NotFound(backend.views.html.error("404", "Not found...")))
    }
  }

  def doTeamUpdate(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val res: Future[Future[Result]] = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      exponentOpt <- ExponentRepository.getByUuid(exponentId)
      attendeeOpt <- AttendeeRepository.getByUuid(attendeeId)
    } yield {
      val res2: Option[Future[Result]] = for {
        event <- eventOpt
        exponent <- exponentOpt
        attendee <- attendeeOpt
      } yield {
        teamCreateForm.bindFromRequest.fold(
          formWithErrors => Future(Ok(backend.views.html.Events.Exponents.Team.update(formWithErrors, attendee, event, exponent))),
          formData => AttendeeRepository.update(attendeeId, AttendeeCreateData.merge(attendee, formData)).map {
            _.map { updatedElt =>
              Redirect(backend.controllers.routes.Exponents.teamDetails(eventId, exponentId, attendeeId)).flashing("success" -> s"${updatedElt.name} a été modifié")
            }.getOrElse {
              InternalServerError(backend.views.html.Events.Exponents.Team.update(teamCreateForm.fill(formData), attendee, event, exponent)).flashing("error" -> s"Impossible de modifier ${attendee.name}")
            }
          })
      }
      res2.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
    res.flatMap(identity)
  }

  def doTeamLeave(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    ExponentRepository.removeTeamMember(exponentId, attendeeId).map { r =>
      Redirect(req.headers("referer"))
    }
  }

  // def doTeamInvite(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>
  // def doTeamBan(eventId: String, exponentId: String, attendeeId: String) = SecuredAction.async { implicit req =>

}
