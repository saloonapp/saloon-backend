package conferences.controllers

import common.repositories.conference.{PersonRepository, ConferenceRepository, PresentationRepository}
import conferences.models._
import org.joda.time.DateTime
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Presentations extends Controller {
  val presentationForm = Form(PresentationData.fields)

  def search(q: Option[String]) = Action.async { implicit req =>
    for {
      presentationList <- PresentationRepository.find(PresentationRepository.buildSearchFilter(q), Json.obj("start" -> -1, "title" -> 1))
      speakerMap <- PersonRepository.findByIds(presentationList.flatMap(_.speakers).distinct)
      conferenceMap <- ConferenceRepository.findByIds(presentationList.map(_.conferenceId).distinct)
    } yield {
      Ok(conferences.views.html.presentation.list(presentationList, conferenceMap, speakerMap))
    }
  }

  def detail(cId: ConferenceId, pId: PresentationId) = detailPrivate(cId, pId, None)
  def detailVersion(cId: ConferenceId, pId: PresentationId, created: Long) = detailPrivate(cId, pId, Some(created))
  private def detailPrivate(cId: ConferenceId, pId: PresentationId, created: Option[Long]) = Action.async { implicit req =>
    for {
      conferenceOpt <- ConferenceRepository.get(cId)
      presentationOpt <- created.map(c => PresentationRepository.get(cId, pId, new DateTime(c))).getOrElse(PresentationRepository.get(cId, pId))
      speakerMap <- presentationOpt.map(presentation => PersonRepository.findByIds(presentation.speakers)).getOrElse(Future(Map()))
    } yield {
      conferenceOpt.flatMap { conference =>
        presentationOpt.map { presentation =>
          Ok(conferences.views.html.presentation.detail(conference, presentation, speakerMap.map(_._2).toList))
        }
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }

  def create(cIdOpt: Option[ConferenceId]) = Action.async { implicit req =>
    play.Logger.info("cIdOpt: "+cIdOpt)
    cIdOpt.map { cId =>
      ConferenceRepository.get(cId).flatMap { conferenceOpt =>
        conferenceOpt.map { conference =>
          formView(Some(cId), presentationForm.fill(PresentationData.empty(conference)))
        }.getOrElse {
          Future(NotFound("Not Found !"))
        }
      }
    }.getOrElse {
      formView(None, presentationForm.bindFromRequest.discardingErrors)
    }
  }

  def doCreate(cIdOpt: Option[ConferenceId]) = Action.async { implicit req =>
    presentationForm.bindFromRequest.fold(
      formWithErrors => formView(cIdOpt, formWithErrors),
      formData => {
        PresentationData.toModel(formData).flatMap { presentation =>
          PresentationRepository.insert(presentation).map { success =>
            Redirect(conferences.controllers.routes.Presentations.detail(presentation.conferenceId, presentation.id))
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
        formView(Some(cId), presentationForm.fill(PresentationData.fromModel(presentation)))
      }.getOrElse {
        Future(NotFound("Not Found !"))
      }
    }
  }

  def doEdit(cId: ConferenceId, pId: PresentationId) = Action.async { implicit req =>
    presentationForm.bindFromRequest.fold(
      formWithErrors => formView(Some(cId), formWithErrors),
      formData => {
        PresentationData.toModel(formData).flatMap { presentation =>
          PresentationRepository.update(cId, pId, presentation).map { success =>
            Redirect(conferences.controllers.routes.Presentations.detail(cId, pId))
          }
        }
      }
    )
  }

  private def formView(cIdOpt: Option[ConferenceId], form: Form[PresentationData])(implicit req: RequestHeader): Future[Result] = {
    // TODO : autocomplete/suggest for room & duration
    val conferenceOptFut = cIdOpt.map { cId => ConferenceRepository.get(cId) }.getOrElse(Future(None))
    val conferenceListFut = cIdOpt.map { cId => Future(List()) }.getOrElse(ConferenceRepository.find())
    val personsFut = PersonRepository.find()
    val tagsFut = PresentationRepository.getTags()
    val roomsFut = cIdOpt.map { cId => PresentationRepository.getRooms(cId) }.getOrElse(Future(List()))
    for {
      conferenceOpt <- conferenceOptFut
      conferenceList <- conferenceListFut
      persons <- personsFut
      tags <- tagsFut
      rooms <- roomsFut
    } yield {
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.presentation.form(Some(conference), form, conferenceList, persons, tags, rooms))
      }.getOrElse {
        Ok(conferences.views.html.presentation.form(None, form, conferenceList, persons, tags, rooms))
      }
    }
  }
}
