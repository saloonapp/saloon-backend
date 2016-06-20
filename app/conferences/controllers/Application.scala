package conferences.controllers

import common.repositories.conference.ConferenceRepository
import conferences.models.{ConferenceCfp, ConferenceId, ConferenceMetrics, ConferenceData}
import org.joda.time.DateTime
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object Application extends Controller {
  val conferenceForm = Form(ConferenceData.fields)
  val conf = ConferenceData(
    id = None,
    name = "Test conf",
    description = Some("C'est une super conf\nViendez tous !"),
    start = new DateTime(),
    end = new DateTime(),
    siteUrl = "http://localhost:9000/conferences/create",
    videosUrl = None,
    tags = "web, java, tech",
    venue = None,
    cfp = Some(ConferenceCfp(
      siteUrl = "http://cfp.devoxx.fr/",
      start = None,
      end = new DateTime())),
    tickets = None,
    metrics = Some(ConferenceMetrics(
      attendeeCount = Some(1),
      sessionCount = Some(2),
      sinceYear = Some(3))),
    social = None)

  def list = Action.async { implicit req =>
    ConferenceRepository.find(Json.obj("end" -> Json.obj("$gte" -> new DateTime()))).map { conferenceList =>
      Ok(conferences.views.html.conferenceList("future", conferenceList))
    }
  }
  def search(section: Option[String], before: Option[String], after: Option[String], tags: Option[String]) = Action.async { implicit req =>
    val filter = buildFilter(before, after, tags)
    play.Logger.info("filter: "+filter)
    ConferenceRepository.find(filter).map { conferenceList =>
      Ok(conferences.views.html.conferenceList(section.getOrElse("search"), conferenceList))
    }
  }
  def detail(id: ConferenceId) = Action.async { implicit req =>
    ConferenceRepository.get(id).map { conferenceOpt =>
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.conferenceDetail(conference))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }
  def create = Action { implicit req =>
    Ok(conferences.views.html.conferenceForm(conferenceForm))
  }
  def doCreate = Action.async { implicit req =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(conferences.views.html.conferenceForm(formWithErrors))),
      formData => {
        val conference = ConferenceData.toModel(formData)
        ConferenceRepository.insert(conference).map { createdOpt =>
          Redirect(conferences.controllers.routes.Application.detail(conference.id))
        }
      }
    )
  }
  def edit(id: ConferenceId) = editPrivate(id, None)
  def editVersion(id: ConferenceId, created: Long) = editPrivate(id, Some(created))
  private def editPrivate(id: ConferenceId, created: Option[Long]) = Action.async { implicit req =>
    val conferenceFut = created.map(c => ConferenceRepository.get(id, new DateTime(c))).getOrElse(ConferenceRepository.get(id))
    conferenceFut.map { conferenceOpt =>
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.conferenceForm(conferenceForm.fill(ConferenceData.fromModel(conference))))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }
  def doEdit(id: ConferenceId) = Action.async { implicit req =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(conferences.views.html.conferenceForm(formWithErrors))),
      formData => {
        ConferenceRepository.update(id, ConferenceData.toModel(formData)).map { updatedOpt =>
          Redirect(conferences.controllers.routes.Application.detail(id))
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
      Redirect(conferences.controllers.routes.Application.history(id))
    }
  }
  def doDeleteAll(id: ConferenceId) = Action.async { implicit req =>
    ConferenceRepository.delete(id).map { _ =>
      Redirect(conferences.controllers.routes.Application.list)
    }
  }

  private def buildFilter(before: Option[String], after: Option[String], tags: Option[String]): JsObject = {
    def reduce(l: List[Option[JsObject]]): Option[JsObject] = if(l.flatten.length > 0) Some(l.flatten.reduceLeft(_ ++ _)) else None
    def parseDate(d: String): Option[DateTime] = Try(DateTime.parse(d)).toOption
    def buildDateFilter(before: Option[String], after: Option[String]): Option[JsObject] = {
      val filterBefore = before.flatMap(parseDate).map(d => Json.obj("$lte" -> d))
      val filterAfter = after.flatMap(parseDate).map(d => Json.obj("$gte" -> d))
      reduce(List(filterBefore, filterAfter)).map(f => Json.obj("end" -> f))
    }
    def buildTagFilter(tags: Option[String]): Option[JsObject] = {
      tags.map(t => Json.obj("tags" -> Json.obj("$in" -> t.split(",").map(_.trim))))
    }
    reduce(List(
      buildDateFilter(before, after),
      buildTagFilter(tags)
    )).getOrElse(Json.obj())
  }
}
