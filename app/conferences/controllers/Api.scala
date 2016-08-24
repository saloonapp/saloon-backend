package conferences.controllers

import common.repositories.conference.{PresentationRepository, ConferenceRepository}
import conferences.models.ConferenceData
import play.api.libs.json.Json
import play.api.mvc.{Controller, Action}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Api extends Controller {
  def list = Action.async { implicit req =>
    ConferenceRepository.find().map { conferences =>
      Ok(Json.obj("result" -> conferences.map(c => c.copy(createdBy = c.createdBy.filter(_.public)))))
    }
  }

  def search(q: Option[String], period: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]) = Action.async { implicit req =>
    ConferenceRepository.find(ConferenceRepository.buildSearchFilter(q, period, before, after, tags, cfp, tickets, videos)).map { conferences =>
      Ok(Json.obj("result" -> conferences.map(c => c.copy(createdBy = c.createdBy.filter(_.public)))))
    }
  }
  def speaker(name: String) = Action.async { implicit req =>
    PresentationRepository.getSpeakers(Json.obj("name" -> name)).map { speakers =>
      Ok(Json.obj("result" -> speakers))
    }
  }

  // add conference
  def addConference() = Action.async(parse.json) { implicit req =>
    req.body.asOpt[ConferenceData].map { conferenceData =>
      val conference = ConferenceData.toModel(conferenceData)
      Future(Ok(Json.obj(
        "conference" -> conference)))
    }.getOrElse {
      Future(BadRequest("wrong data"))
    }
  }
  // edit conference
  // add talk
  // edit talk
  // add speaker (when added)
  // edit speaker (when added)
}
