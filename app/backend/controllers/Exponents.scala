package backend.controllers

import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.Exponent
import common.models.event.ExponentId
import common.models.user.User
import common.models.utils.Page
import common.services.FileExporter
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import backend.forms.ExponentCreateData
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

  def list(eventId: EventId, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
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

  def details(eventId: EventId, exponentId: ExponentId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withExponent(exponentId) { exponent =>
        AttendeeRepository.findByUuids(exponent.info.team).map { team =>
          Ok(backend.views.html.Events.Exponents.details(exponent, team, event))
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
      formData => ExponentRepository.insert(ExponentCreateData.toModel(formData)).flatMap {
        _.map { exponent =>
          Future(Redirect(backend.controllers.routes.Exponents.details(eventId, exponent.uuid)).flashing("success" -> s"Exposant '${exponent.name}' créé !"))
        }.getOrElse {
          createView(createForm.fill(formData), eventId, InternalServerError)
        }
      })
  }

  def update(eventId: EventId, exponentId: ExponentId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withExponent(exponentId) { exponent =>
      updateView(createForm.fill(ExponentCreateData.fromModel(exponent)), exponent, eventId)
    }
  }

  def doUpdate(eventId: EventId, exponentId: ExponentId) = SecuredAction.async { implicit req =>
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

  def doDelete(eventId: EventId, exponentId: ExponentId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withExponent(exponentId) { exponent =>
      // TODO : What to do with linked attendees ?
      //	- delete an exponent only if it has no team member ?
      // 	- delete thoses who are not linked with other elts (exponents / sessions)
      //	- ask which one to delete (showing other links)
      ExponentRepository.delete(exponentId).map { res =>
        Redirect(backend.controllers.routes.Exponents.list(eventId)).flashing("success" -> s"Suppression de l'exposant '${exponent.name}'")
      }
    }
  }

  def doFileExport(eventId: EventId) = SecuredAction.async { implicit req =>
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
   * Private methods
   */

  private def createView(createForm: Form[ExponentCreateData], eventId: EventId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      Future(status(backend.views.html.Events.Exponents.create(createForm, event)))
    }
  }

  private def updateView(createForm: Form[ExponentCreateData], exponent: Exponent, eventId: EventId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      Future(status(backend.views.html.Events.Exponents.update(createForm.fill(ExponentCreateData.fromModel(exponent)), exponent, event)))
    }
  }

}
