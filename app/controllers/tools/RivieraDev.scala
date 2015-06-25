package controllers.tools

import models.values.Address
import models.values.DataSource
import models.values.Link
import models.event.Event
import models.event.EventImages
import models.event.EventInfo
import models.event.EventInfoSocial
import models.event.EventInfoSocialTwitter
import models.event.EventEmail
import models.event.EventConfig
import models.event.EventMeta
import models.event.Session
import models.event.SessionImages
import models.event.SessionInfo
import models.event.SessionMeta
import models.event.Exponent
import models.event.ExponentImages
import models.event.ExponentInfo
import models.event.ExponentConfig
import models.event.ExponentMeta
import models.event.Person
import models.event.Person._
import models.event.PersonSocial
import tools.scrapers.meta.MetaScraper
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current
import org.joda.time.DateTime

object RivieraDev extends Controller {

  case class RivieraDevEvent(name: String, description: String, logoUrl: String, siteUrl: String, start: Option[DateTime], end: Option[DateTime], address: Address, price: String, priceUrl: String, twitterAccount: Option[String], tags: List[String]) {
    def toEvent(): Event = Event(Repository.generateUuid(), this.name, this.description, EventImages(this.logoUrl, ""), EventInfo(this.siteUrl, this.start, this.end, this.address, Link(this.price, this.priceUrl), EventInfoSocial(EventInfoSocialTwitter(None, this.twitterAccount))), EventEmail(None), EventConfig(None, true), EventMeta(List(), Some("/api/v1/tools/scrapers/events/rivieradev/formated"), Some(DataSource(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/general")), new DateTime(), new DateTime()))
  }
  case class RivieraDevPerson(name: String, description: String, company: Option[String], avatar: String, profilUrl: Option[String], social: Option[PersonSocial]) {
    def toPerson(): Person = Person(this.name, this.description, this.company.getOrElse(""), this.avatar, None, this.profilUrl.getOrElse(""), this.social.getOrElse(PersonSocial(None, None, None, None, None)))
  }
  case class RivieraDevSession(name: String, description: Option[String], start: Option[DateTime], end: Option[DateTime], category: Option[String], place: Option[String], speakers: List[RivieraDevPerson]) {
    def toSession(eventId: String): Session = Session(Repository.generateUuid(), eventId, this.name, this.description.getOrElse(""), SessionImages(""), SessionInfo("", this.category.getOrElse(""), this.place.getOrElse(""), this.start, this.end, List() /* TODO this.speakers.map(_.toPerson())*/ , None, None), SessionMeta(Some(DataSource(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/talks")), new DateTime(), new DateTime()))
  }
  case class RivieraDevExponent(name: String, description: String, logoUrl: String, siteUrl: String, sponsor: Boolean) {
    def toExponent(eventId: String): Exponent = Exponent(Repository.generateUuid(), eventId, this.name, this.description, ExponentImages(this.logoUrl, this.logoUrl), ExponentInfo(this.siteUrl, "", List(), None, this.sponsor), ExponentConfig(false), ExponentMeta(Some(DataSource(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/sponsors")), new DateTime(), new DateTime()))
  }
  implicit val formatRivieraDevEvent = Json.format[RivieraDevEvent]
  implicit val formatRivieraDevPerson = Json.format[RivieraDevPerson]
  implicit val formatRivieraDevSession = Json.format[RivieraDevSession]
  implicit val formatRivieraDevExponent = Json.format[RivieraDevExponent]

  def loadRivieraDev() = Action.async { implicit req =>
    for {
      general <- WS.url("http://www.rivieradev.fr/apiv1/general").get()
      talks <- WS.url("http://www.rivieradev.fr/apiv1/talks").get()
      sponsors <- WS.url("http://www.rivieradev.fr/apiv1/sponsors").get()
    } yield {
      val event = general.json.as[RivieraDevEvent].toEvent()
      val sessions = talks.json.as[List[RivieraDevSession]].map(_.toSession(event.uuid))
      val exponents = sponsors.json.as[List[RivieraDevExponent]].map(_.toExponent(event.uuid))
      EventRepository.insert(event)
      SessionRepository.bulkInsert(sessions)
      ExponentRepository.bulkInsert(exponents)
      Ok
    }
  }

  def getFullEventFormated() = Action.async { implicit req =>
    for {
      general <- WS.url("http://www.rivieradev.fr/apiv1/general").get()
      talks <- WS.url("http://www.rivieradev.fr/apiv1/talks").get()
      sponsors <- WS.url("http://www.rivieradev.fr/apiv1/sponsors").get()
    } yield {
      val event = general.json.as[RivieraDevEvent].toEvent()
      val sessions = talks.json.as[List[RivieraDevSession]].map(_.toSession(event.uuid))
      val exponents = sponsors.json.as[List[RivieraDevExponent]].map(_.toExponent(event.uuid))

      Ok(Json.toJson(event).as[JsObject] ++ Json.obj(
        "sessions" -> sessions,
        "exponents" -> exponents))
    }
  }

}
