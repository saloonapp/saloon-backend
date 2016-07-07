package conferences.controllers

import common.{Utils, Defaults}
import common.repositories.conference.ConferenceRepository
import common.services._
import conferences.models._
import conferences.services.TwittFactory
import org.joda.time.DateTime
import play.api.data.Form
import play.api.libs.json.{JsNull, JsObject, Json}
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object Application extends Controller {
  val conferenceForm = Form(ConferenceData.fields)

  def list = Action.async { implicit req =>
    val conferenceListFut = ConferenceRepository.find(Json.obj("end" -> Json.obj("$gte" -> new DateTime().withTime(0, 0, 0, 0))), Json.obj("start" -> 1))
    //val tagsFut = ConferenceRepository.getTags()
    for {
      conferenceList <- conferenceListFut
      //tags <- tagsFut
    } yield Ok(conferences.views.html.conferenceList("future", conferenceList))
  }

  def search(section: Option[String], q: Option[String], period: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]) = Action.async { implicit req =>
    val conferenceListFut = ConferenceRepository.find(ConferenceRepository.buildSearchFilter(q, period, before, after, tags, cfp, tickets, videos))
    //val tagsFut = ConferenceRepository.getTags()
    for {
      conferenceList <- conferenceListFut
      //tags <- tagsFut
    } yield Ok(conferences.views.html.conferenceList(section.getOrElse("search"), conferenceList))
  }

  def calendar = Action { implicit req =>
    Ok(conferences.views.html.conferenceCalendar())
  }

  def fullHistory = Action.async { implicit req =>
    ConferenceRepository.findHistory().map { conferenceList =>
      Ok(conferences.views.html.conferenceListHistory(conferenceList))
    }
  }

  def detail(id: ConferenceId) = detailPrivate(id, None)
  def detailVersion(id: ConferenceId, created: Long) = detailPrivate(id, Some(created))
  private def detailPrivate(id: ConferenceId, created: Option[Long]) = Action.async { implicit req =>
    created.map(c => ConferenceRepository.get(id, new DateTime(c))).getOrElse(ConferenceRepository.get(id)).map { conferenceOpt =>
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.conferenceDetail(conference))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }

  def create = Action.async { implicit req =>
    ConferenceRepository.getTags().map { tags =>
      Ok(conferences.views.html.conferenceForm(conferenceForm, tags))
    }
  }

  def doCreate = Action.async { implicit req =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => ConferenceRepository.getTags().map { tags =>
        BadRequest(conferences.views.html.conferenceForm(formWithErrors, tags))
      },
      formData => {
        val conference = ConferenceData.toModel(formData)
        ConferenceRepository.insert(conference).map { success =>
          if(conference.start.isAfterNow){
            TwitterSrv.twitt(TwittFactory.newConference(conference))
          }
          Redirect(conferences.controllers.routes.Application.detail(conference.id))
        }
      }
    )
  }

  def edit(id: ConferenceId) = editPrivate(id, None)
  def editVersion(id: ConferenceId, created: Long) = editPrivate(id, Some(created))
  private def editPrivate(id: ConferenceId, created: Option[Long]) = Action.async { implicit req =>
    val conferenceOptFut = created.map(c => ConferenceRepository.get(id, new DateTime(c))).getOrElse(ConferenceRepository.get(id))
    val tagsFut = ConferenceRepository.getTags()
    for {
      conferenceOpt <- conferenceOptFut
      tags <- tagsFut
    } yield {
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.conferenceForm(conferenceForm.fill(ConferenceData.fromModel(conference)), tags))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }

  def doEdit(id: ConferenceId) = Action.async { implicit req =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => ConferenceRepository.getTags().map { tags =>
        BadRequest(conferences.views.html.conferenceForm(formWithErrors, tags))
      },
      formData => {
        val conference = ConferenceData.toModel(formData)
        ConferenceRepository.get(id).flatMap { oldConferenceOpt =>
          ConferenceRepository.update(id, conference).map { success =>
            oldConferenceOpt.filter(_.videosUrl.isEmpty && conference.videosUrl.isDefined).map { _ =>
              TwitterSrv.twitt(TwittFactory.publishVideos(conference))
            }
            oldConferenceOpt.filter(_.cfp.isEmpty && conference.cfp.map(_.end.isAfterNow).getOrElse(false)).map { _ =>
              TwitterSrv.twitt(TwittFactory.openCfp(conference))
            }
            Redirect(conferences.controllers.routes.Application.detail(id))
          }
        }
      }
    )
  }

  def history(id: ConferenceId) = Action.async { implicit req =>
    ConferenceRepository.getHistory(id).map { conferenceList =>
      if(conferenceList.length > 0){
        Ok(conferences.views.html.conferenceHistory(conferenceList))
      } else {
        NotFound("Not Found !")
      }
    }
  }

  def doDelete(id: ConferenceId, created: Long) = Action.async { implicit req =>
    ConferenceRepository.deleteVersion(id, new DateTime(created)).map { _ =>
      Redirect(req.headers("referer"))
    }
  }

  /*def doDeleteAll(id: ConferenceId) = Action.async { implicit req =>
    ConferenceRepository.delete(id).map { _ =>
      Redirect(conferences.controllers.routes.Application.list)
    }
  }*/

  def about = Action { implicit req =>
    Ok(conferences.views.html.about())
  }
}
