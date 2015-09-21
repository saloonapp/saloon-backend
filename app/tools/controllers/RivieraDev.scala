package tools.controllers

import common.models.values.Address
import common.models.values.Link
import common.models.values.Source
import common.models.event.Event
import common.models.event.EventImages
import common.models.event.EventInfo
import common.models.event.EventInfoSocial
import common.models.event.EventInfoSocialTwitter
import common.models.event.EventEmail
import common.models.event.EventConfig
import common.models.event.EventMeta
import common.models.event.Attendee
import common.models.event.Attendee._
import common.models.event.AttendeeImages
import common.models.event.AttendeeInfo
import common.models.event.AttendeeSocial
import common.models.event.AttendeeMeta
import common.models.event.Session
import common.models.event.SessionImages
import common.models.event.SessionInfo
import common.models.event.SessionMeta
import common.models.event.Exponent
import common.models.event.ExponentImages
import common.models.event.ExponentInfo
import common.models.event.ExponentConfig
import common.models.event.ExponentMeta
import common.repositories.Repository
import common.repositories.event.EventRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import tools.scrapers.meta.MetaScraper
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current
import org.joda.time.DateTime

object RivieraDev extends Controller {

  /*case class RivieraDevEvent(name: String, description: String, logoUrl: String, siteUrl: String, start: Option[DateTime], end: Option[DateTime], address: Address, price: String, priceUrl: String, twitterAccount: Option[String], tags: List[String]) {
    def toEvent(): Event = Event(Repository.generateUuid(), this.name, this.description, EventImages(this.logoUrl, ""), EventInfo(this.siteUrl, this.start, this.end, this.address, Link(this.price, this.priceUrl), EventInfoSocial(EventInfoSocialTwitter(None, this.twitterAccount))), EventEmail(None), EventConfig(None, true), EventMeta(List(), Some("/api/v1/tools/scrapers/events/rivieradev/formated"), Some(Source(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/general")), new DateTime(), new DateTime()))
  }
  case class RivieraDevAttendee(name: String, description: String, company: Option[String], avatar: String, profilUrl: Option[String], social: AttendeeSocial) {
    def toAttendee(eventId: String): Attendee = Attendee(Repository.generateUuid(), eventId, this.name, this.description, AttendeeImages(this.avatar), AttendeeInfo("speaker", "", this.company.getOrElse(""), this.profilUrl), social, AttendeeMeta(Some(Source(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/talks")), new DateTime(), new DateTime()))
  }
  case class RivieraDevSession(name: String, description: Option[String], start: Option[DateTime], end: Option[DateTime], category: Option[String], place: Option[String], speakers: List[RivieraDevAttendee]) {
    def toSession(eventId: String): Session = Session(Repository.generateUuid(), eventId, this.name, this.description.getOrElse(""), SessionImages(""), SessionInfo("", this.category.getOrElse(""), this.place.getOrElse(""), this.start, this.end, List() /* TODO this.speakers.map(_.toPerson())*/ , None, None), SessionMeta(Some(Source(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/talks")), new DateTime(), new DateTime()))
  }
  case class RivieraDevExponent(name: String, description: String, logoUrl: String, siteUrl: String, sponsor: Boolean) {
    def toExponent(eventId: String): Exponent = Exponent(Repository.generateUuid(), eventId, this.name, this.description, ExponentImages(this.logoUrl, this.logoUrl), ExponentInfo(this.siteUrl, "", List(), None, this.sponsor), ExponentConfig(false), ExponentMeta(Some(Source(this.name, "RivieraDev API", "http://www.rivieradev.fr/apiv1/sponsors")), new DateTime(), new DateTime()))
  }
  implicit val formatRivieraDevEvent = Json.format[RivieraDevEvent]
  implicit val formatRivieraDevAttendee = Json.format[RivieraDevAttendee]
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
  }*/

}
