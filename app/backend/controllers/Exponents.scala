package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.utils.Page
import common.services.FileExporter
import common.repositories.event.EventRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.AttendeeRepository
import backend.forms.ExponentCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
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
          .map { event => Ok(backend.views.html.Exponents.list(eltPage, event)) }
          .getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
      }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val futureData = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- ExponentRepository.getByUuid(uuid)
    } yield (eltOpt, eventOpt)
    futureData.flatMap {
      case (eltOpt, eventOpt) =>
        eltOpt.flatMap { elt =>
          eventOpt.map { event =>
            AttendeeRepository.findByUuids(elt.info.team).map { team => Ok(backend.views.html.Exponents.details(elt, team, event)) }
          }
        }.getOrElse { Future(NotFound(backend.views.html.error("404", "Event not found..."))) }
    }
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      eventOpt
        .map { event => Ok(backend.views.html.Exponents.create(createForm, allAttendees, event)) }
        .getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        createForm.bindFromRequest.fold(
          formWithErrors => AttendeeRepository.findByEvent(eventId).map { allAttendees => BadRequest(backend.views.html.Exponents.create(formWithErrors, allAttendees, event)) },
          formData => ExponentRepository.insert(ExponentCreateData.toModel(formData)).flatMap {
            _.map { elt =>
              Future(Redirect(backend.controllers.routes.Exponents.details(eventId, elt.uuid)).flashing("success" -> s"Exposant '${elt.name}' créé !"))
            }.getOrElse {
              AttendeeRepository.findByEvent(eventId).map { allAttendees =>
                InternalServerError(backend.views.html.Exponents.create(createForm.fill(formData), allAttendees, event)).flashing("error" -> s"Impossible de créer l'exposant '${formData.name}'")
              }
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
      allAttendees <- AttendeeRepository.findByEvent(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Exponents.update(createForm.fill(ExponentCreateData.fromModel(elt)), elt, allAttendees, event)) }
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
            formWithErrors => AttendeeRepository.findByEvent(eventId).map { allAttendees => BadRequest(backend.views.html.Exponents.update(formWithErrors, elt, allAttendees, event)) },
            formData => ExponentRepository.update(uuid, ExponentCreateData.merge(elt, formData)).flatMap {
              _.map { updatedElt =>
                Future(Redirect(backend.controllers.routes.Exponents.details(eventId, updatedElt.uuid)).flashing("success" -> s"L'exposant '${updatedElt.name}' a bien été modifié"))
              }.getOrElse {
                AttendeeRepository.findByEvent(eventId).map { allAttendees =>
                  InternalServerError(backend.views.html.Exponents.update(createForm.fill(formData), elt, allAttendees, event)).flashing("error" -> s"Impossible de modifier l'exposant '${elt.name}'")
                }
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

}
