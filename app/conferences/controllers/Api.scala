package conferences.controllers

import common.repositories.conference.{PersonRepository, PresentationRepository, ConferenceRepository}
import conferences.models.PersonData
import org.joda.time.LocalDate
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{Result, Controller, Action}
import reactivemongo.api.commands.WriteResult
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Api extends Controller {
  def searchConferences(q: Option[String], period: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]) = Action.async { implicit req =>
    val isSearch = List(q, period, before, after, tags, cfp, tickets, videos).map(_.filter(_ != "")).flatten.headOption
    val conferenceListFut = isSearch.map { _ =>
      val reverseSort = List(q, before, tags, videos).map(_.filter(_ != "")).flatten.headOption
      val sort = reverseSort.map(_ => Json.obj("start" -> -1)).getOrElse(Json.obj("start" -> 1))
      ConferenceRepository.find(ConferenceRepository.buildSearchFilter(q, period, before, after, tags, cfp, tickets, videos), sort)
    }.getOrElse {
      ConferenceRepository.find(Json.obj("end" -> Json.obj("$gte" -> new LocalDate())), Json.obj("start" -> 1))
    }
    conferenceListFut.map { conferenceList =>
      Ok(Json.obj("result" -> conferenceList))
    }
  }
  /*def createConference() = Action.async(parse.json) { implicit req =>
    req.body.validate[ConferenceData] match {
      case JsSuccess(conferenceData, path) => {
        val conference = ConferenceData.toModel(conferenceData)
        Future(Created(Json.obj(
          "result" -> conference)))
      }
      case JsError(err) => Future(validationError(err))
    }
  }*/


  def searchCfps(q: Option[String]) = Action.async { implicit req =>
    ConferenceRepository.find(ConferenceRepository.buildSearchFilter(q, None, None, None, None, Some("on"), None, None), Json.obj("cfp.end" -> 1)).map { conferenceList =>
      Ok(Json.obj("result" -> conferenceList))
    }
  }

  def searchPresentations(q: Option[String]) = Action.async { implicit req =>
    PresentationRepository.find(PresentationRepository.buildSearchFilter(q), Json.obj("start" -> -1, "title" -> 1)).map { presentations =>
      Ok(Json.obj("result" -> presentations))
    }
  }

  def searchPersons(q: Option[String]) = Action.async { implicit req =>
    PersonRepository.find(PersonRepository.buildSearchFilter(q), Json.obj("name" -> 1)).map { persons =>
      Ok(Json.obj("result" -> persons))
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
