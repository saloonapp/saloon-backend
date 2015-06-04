package controllers.tools

import models._
import tools.scrapers.meta.MetaScraper
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.Play.current
import org.joda.time.DateTime

object RivieraDev extends Controller {

  case class RivieraDevEvent(name: String, description: String, logoUrl: String, siteUrl: String, start: Option[DateTime], end: Option[DateTime], address: Address, price: String, priceUrl: String, twitterAccount: Option[String], tags: List[String]) {
    def toEvent(): Event = Event(Repository.generateUuid(), this.name, this.description, this.logoUrl, "", this.siteUrl, this.start, this.end, this.address, this.price, this.priceUrl, None, this.twitterAccount, this.tags, true, None, new DateTime(), new DateTime())
  }
  case class RivieraDevPerson(name: String, description: String, company: Option[String], avatar: String, profilUrl: Option[String], social: Option[PersonSocial]) {
    def toPerson(): Person = Person(this.name, this.description, this.company.getOrElse(""), this.avatar, this.profilUrl.getOrElse(""), this.social.getOrElse(PersonSocial(None, None, None, None, None)))
  }
  case class RivieraDevSession(name: String, description: String, start: Option[DateTime], end: Option[DateTime], category: Option[String], speakers: List[RivieraDevPerson]) {
    def toSession(eventId: String): models.Session = models.Session(Repository.generateUuid(), eventId, this.name, this.description, "", this.category.getOrElse(""), "", this.start, this.end, this.speakers.map(_.toPerson()), List(), None, new DateTime(), new DateTime())
  }
  case class RivieraDevExponent(name: String, description: String, logoUrl: String, siteUrl: String, sponsor: Boolean) {
    def toExponent(eventId: String): Exponent = Exponent(Repository.generateUuid(), eventId, this.name, this.description, this.logoUrl, "", this.siteUrl: String, None, List(), None, this.sponsor, List(), List(), None, new DateTime(), new DateTime())
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

}
