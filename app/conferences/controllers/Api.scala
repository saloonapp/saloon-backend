package conferences.controllers

import common.repositories.conference.{PersonRepository, PresentationRepository, ConferenceRepository}
import common.services.TwitterSrv
import conferences.models.{PersonId, PersonData, ConferenceData}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{Result, Controller, Action}
import reactivemongo.api.commands.WriteResult
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Api extends Controller {
  def listConferences = Action.async { implicit req =>
    ConferenceRepository.find().map { conferences =>
      Ok(Json.obj("result" -> conferences.map(c => c.copy(createdBy = c.createdBy.filter(_.public)))))
    }
  }
  def searchConferences(q: Option[String], period: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]) = Action.async { implicit req =>
    ConferenceRepository.find(ConferenceRepository.buildSearchFilter(q, period, before, after, tags, cfp, tickets, videos)).map { conferences =>
      Ok(Json.obj("result" -> conferences.map(c => c.copy(createdBy = c.createdBy.filter(_.public)))))
    }
  }
  def createConference() = Action.async(parse.json) { implicit req =>
    req.body.validate[ConferenceData] match {
      case JsSuccess(conferenceData, path) => {
        val conference = ConferenceData.toModel(conferenceData)
        Future(Ok(Json.obj(
          "conference" -> conference)))
      }
      case JsError(err) => Future(validationError(err))
    }
  }

  def listPresentations() = Action.async { implicit req =>
    PresentationRepository.find().map { presentations =>
      Ok(Json.obj("result" -> presentations.map(p => p.copy(createdBy = p.createdBy.filter(_.public)))))
    }
  }

  def listPersons() = Action.async { implicit req =>
    PersonRepository.find().map { persons =>
      Ok(Json.obj("result" -> persons.map(p => p.copy(createdBy = p.createdBy.filter(_.public)))))
    }
  }
  def createPerson() = Action.async(parse.json) { implicit req =>
    req.body.validate[PersonData] match {
      case JsSuccess(personData, path) => {
        val person = PersonData.toModel(personData).trim
        PersonRepository.insert(person).map { res =>
          if (res.ok) {
            Created(Json.obj(
              "result" -> person))
          } else {
            dbError(res)
          }
        }
      }
      case JsError(err) => Future(validationError(err))
    }
  }

  private def validationError(err: Seq[(JsPath, Seq[ValidationError])]): Result = {
    BadRequest(Json.obj(
      "error" -> Json.toJson(err.map { case (path, errors) =>
        (path.toJsonString, errors.map(_.message))
      }.toMap)))
  }
  private def dbError(res: WriteResult): Result = {
    InternalServerError(Json.obj(
      "error" -> Json.obj(
        "code" -> res.code,
        "message" -> res.message)))
  }
}
