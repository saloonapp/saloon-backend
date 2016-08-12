package conferences.controllers

import common.repositories.conference.{ConferenceRepository, PresentationRepository}
import conferences.models.{PresentationId, ConferenceId, PresentationData}
import org.joda.time.DateTime
import play.api.data.Form
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Presentation extends Controller {
  val presentationForm = Form(PresentationData.fields)

  def detail(cId: ConferenceId, pId: PresentationId) = detailPrivate(cId, pId, None)
  def detailVersion(cId: ConferenceId, pId: PresentationId, created: Long) = detailPrivate(cId, pId, Some(created))
  private def detailPrivate(cId: ConferenceId, pId: PresentationId, created: Option[Long]) = Action.async { implicit req =>
    for {
      conferenceOpt <- ConferenceRepository.get(cId)
      presentationOpt <- created.map(c => PresentationRepository.get(cId, pId, new DateTime(c))).getOrElse(PresentationRepository.get(cId, pId))
    } yield {
      conferenceOpt.flatMap { conference =>
        presentationOpt.map { presentation =>
          Ok(conferences.views.html.presentation.detail(conference, presentation))
        }
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }

  def create(cId: ConferenceId) = Action.async { implicit req =>
    formView(cId, presentationForm)
  }

  def doCreate(cId: ConferenceId) = Action.async { implicit req =>
    presentationForm.bindFromRequest.fold(
      formWithErrors => formView(cId, formWithErrors),
      formData => {
        PresentationData.toModel(formData).flatMap { presentation =>
          PresentationRepository.insert(presentation).map { success =>
            Redirect(conferences.controllers.routes.Presentation.detail(cId, presentation.id))
          }
        }
      }
    )
  }

  def edit(cId: ConferenceId, pId: PresentationId) = editPrivate(cId, pId, None)
  def editVersion(cId: ConferenceId, pId: PresentationId, created: Long) = editPrivate(cId, pId, Some(created))
  private def editPrivate(cId: ConferenceId, pId: PresentationId, created: Option[Long]) = Action.async { implicit req =>
    created.map(c => PresentationRepository.get(cId, pId, new DateTime(c))).getOrElse(PresentationRepository.get(cId, pId)).flatMap { presentationOpt =>
      presentationOpt.map { presentation =>
        formView(cId, presentationForm.fill(PresentationData.fromModel(presentation)))
      }.getOrElse {
        Future(NotFound("Not Found !"))
      }
    }
  }

  def doEdit(cId: ConferenceId, pId: PresentationId) = Action.async { implicit req =>
    presentationForm.bindFromRequest.fold(
      formWithErrors => formView(cId, formWithErrors),
      formData => {
        PresentationData.toModel(formData).flatMap { presentation =>
          PresentationRepository.update(cId, pId, presentation).map { success =>
            Redirect(conferences.controllers.routes.Presentation.detail(cId, pId))
          }
        }
      }
    )
  }

  private def formView(cId: ConferenceId, form: Form[PresentationData])(implicit req: RequestHeader): Future[Result] = {
    // TODO : autocomplete/suggest for room, duration & speaker
    val conferenceOptFut = ConferenceRepository.get(cId)
    val tagsFut = PresentationRepository.getTags()
    val roomsFut = PresentationRepository.getRooms(cId)
    for {
      conferenceOpt <- conferenceOptFut
      tags <- tagsFut
      rooms <- roomsFut
    } yield {
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.presentation.form(conference, form, tags, rooms))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }
}
