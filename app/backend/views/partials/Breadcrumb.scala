package backend.views.partials

import common.models.utils.tString
import common.models.values.UUID
import common.models.values.typed.ItemType
import common.models.user.OrganizationId
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.ExponentId
import common.models.event.SessionId
import play.api.mvc.Call
import scala.collection.mutable.MutableList
import scala.collection.mutable.HashMap

object Breadcrumb {
  def buildBreadcrumb(uri: String, titles: Map[UUID, tString]): Option[(List[(Call, String)], String)] = {
    prepare(uri).map { list =>
      val result = new MutableList[(Call, String)]()
      val identifiers = new HashMap[String, String]()
      for (i <- 0 until list.length) {
        result += build(list, i, titles.map { case (key, value) => (key.unwrap, value.unwrap) }, identifiers)
      }
      (result.toList.take(result.length - 1), result.last._2)
    }
  }

  private def prepare(uri: String): Option[List[String]] = {
    val correctUri = (if (uri.startsWith("/backend/")) uri.substring(9) else uri).split("\\?")(0)
    val list = correctUri.split("/").toList
    if (list.length > 0 && !List("welcome", "requests").contains(list(0))) {
      Some(List("home") ++ list)
    } else {
      None
    }
  }

  private val identifier = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})".r
  private def build(list: List[String], index: Int, titles: Map[String, String], identifiers: HashMap[String, String]): (Call, String) = {
    list(index) match {
      case "home" => (backend.controllers.routes.Application.index(), "Accueil")
      case "profile" => (backend.controllers.routes.Profile.details(), "Profil")
      case "organizations" => (backend.controllers.routes.Profile.details(), "Organisations")
      case ItemType.events.value => list(index - 1) match {
        case "admin" => (backend.controllers.admin.routes.Events.list(), "Tous les événements")
        case "eventDirectory" => (backend.controllers.eventDirectory.routes.Events.list(), "Événement")
        case _ => (backend.controllers.routes.Events.list(), "Mes événements")
      }
      case ItemType.attendees.value => (backend.controllers.routes.Attendees.list(getEventId(identifiers)), "Participants")
      case ItemType.exponents.value => (backend.controllers.routes.Exponents.list(getEventId(identifiers)), "Exposants")
      case "team" => (backend.controllers.routes.Exponents.details(getEventId(identifiers), getExponentId(identifiers)), "Équipe")
      case ItemType.sessions.value => (backend.controllers.routes.Sessions.list(getEventId(identifiers)), "Sessions")
      case "speakers" => (backend.controllers.routes.Sessions.details(getEventId(identifiers), getSessionId(identifiers)), "Speakers")
      case "ticketing" => (backend.controllers.routes.Ticketing.details(getEventId(identifiers)), "Ticketing")
      case "create" => list(index - 1) match {
        case ItemType.events.value => (backend.controllers.routes.Events.create(), "Créer un événement")
        case ItemType.attendees.value => (backend.controllers.routes.Attendees.create(getEventId(identifiers)), "Nouveau participant")
        case ItemType.exponents.value => (backend.controllers.routes.Exponents.create(getEventId(identifiers)), "Nouvel exposant")
        case "team" => (backend.controllers.routes.AttendeeTeam.create(getEventId(identifiers), ItemType.exponents, getExponentId(identifiers)), "Nouveau membre")
        case ItemType.sessions.value => (backend.controllers.routes.Sessions.create(getEventId(identifiers)), "Nouvelle session")
        case "speakers" => (backend.controllers.routes.AttendeeTeam.create(getEventId(identifiers), ItemType.sessions, getSessionId(identifiers)), "Nouveau speaker")
        case "scrapers" => (backend.controllers.eventDirectory.routes.Scrapers.create, "Nouveau scraper")
      }
      case "edit" => list(index - 1) match {
        case "profile" => (backend.controllers.routes.Profile.details, "Modification")
        case _ =>
          list(index - 2) match {
            case "organizations" => (backend.controllers.routes.Organizations.update(getOrganizationId(identifiers)), "Modification")
            case ItemType.events.value => (backend.controllers.routes.Events.update(getEventId(identifiers)), "Modification")
            case ItemType.attendees.value => (backend.controllers.routes.Attendees.update(getEventId(identifiers), getAttendeeId(identifiers)), "Modification")
            case ItemType.exponents.value => (backend.controllers.routes.Exponents.update(getEventId(identifiers), getExponentId(identifiers)), "Modification")
            case "team" => (backend.controllers.routes.AttendeeTeam.update(getEventId(identifiers), ItemType.exponents, getExponentId(identifiers), getAttendeeId(identifiers)), "Modification")
            case ItemType.sessions.value => (backend.controllers.routes.Sessions.update(getEventId(identifiers), getSessionId(identifiers)), "Modification")
            case "speakers" => (backend.controllers.routes.AttendeeTeam.update(getEventId(identifiers), ItemType.sessions, getSessionId(identifiers), getAttendeeId(identifiers)), "Modification")
          }
      }
      case "delete" => list(index - 2) match {
        case "organizations" => (backend.controllers.routes.Organizations.delete(getOrganizationId(identifiers)), "Suppression")
      }
      case "config" => list(index - 1) match {
        case "ticketing" => (backend.controllers.routes.Ticketing.configure(getEventId(identifiers)), "Configuration")
      }
      case identifier(id) => {
        setId(identifiers, list(index - 1), id)
        list(index - 1) match {
          case "organizations" => (backend.controllers.routes.Organizations.details(getOrganizationId(identifiers)), titles.get(id).get)
          case ItemType.events.value => list(index - 2) match {
            case "eventDirectory" => (backend.controllers.eventDirectory.routes.Events.details(getId(identifiers, "event")), titles.get(id).get)
            case _ => (backend.controllers.routes.Events.details(getEventId(identifiers)), titles.get(id).get)
          }
          case ItemType.attendees.value => (backend.controllers.routes.Attendees.details(getEventId(identifiers), getAttendeeId(identifiers)), titles.get(id).get)
          case ItemType.exponents.value => (backend.controllers.routes.Exponents.details(getEventId(identifiers), getExponentId(identifiers)), titles.get(id).get)
          case "team" => (backend.controllers.routes.AttendeeTeam.details(getEventId(identifiers), ItemType.exponents, getExponentId(identifiers), getAttendeeId(identifiers)), titles.get(id).get)
          case ItemType.sessions.value => (backend.controllers.routes.Sessions.details(getEventId(identifiers), getSessionId(identifiers)), titles.get(id).get)
          case "speakers" => (backend.controllers.routes.AttendeeTeam.details(getEventId(identifiers), ItemType.sessions, getSessionId(identifiers), getAttendeeId(identifiers)), titles.get(id).get)
          case "scrapers" => (backend.controllers.eventDirectory.routes.Scrapers.list(), "Scrapers")
        }
      }

      case "admin" => (backend.controllers.admin.routes.Application.index(), "Admin")
      case "urlImport" => (backend.controllers.admin.routes.Events.urlImport(), "Importer un événement")
      case "refresh" => list(index - 2) match {
        case "events" => (backend.controllers.admin.routes.Events.refresh(getEventId(identifiers)), "Mettre à jour l'événement")
        case "scrapers" => (backend.controllers.eventDirectory.routes.Scrapers.refresh(getId(identifiers, "scraper")), "Mettre à jour les événements")
      }

      case "eventDirectory" => (backend.controllers.eventDirectory.routes.Application.index(), "Annuaire des événements")
      case "scrapers" => (backend.controllers.eventDirectory.routes.Scrapers.list(), "Scrapers")

      case "mockups" => (backend.controllers.routes.Application.mockups(), "Mockups")
      case "activityWall" => (backend.controllers.routes.Application.mockupActivityWall(), "Activity Wall")
      case "exponentForm" => (backend.controllers.routes.Application.mockupExponentForm(), "Modifier un exposant")
      case "leads" => (backend.controllers.routes.Application.mockupLeads(), "Achat de leads")
      case "scannedAttendees" => (backend.controllers.routes.Application.mockupScannedAttendees(), "Visiteurs scannés")
      case "scannedDocuments" => (backend.controllers.routes.Application.mockupScannedDocuments(), "Documents récoltés")
    }
  }

  private def setId(identifiers: HashMap[String, String], prev: String, id: String): Option[String] = prev match {
    case "organizations" => identifiers.put("organization", id)
    case ItemType.events.value => identifiers.put("event", id)
    case ItemType.attendees.value => identifiers.put("attendee", id)
    case ItemType.exponents.value => identifiers.put("exponent", id)
    case "team" => identifiers.put("attendee", id)
    case ItemType.sessions.value => identifiers.put("session", id)
    case "speakers" => identifiers.put("attendee", id)
    case "scrapers" => identifiers.put("scraper", id)
  }
  private def getOrganizationId(identifiers: HashMap[String, String]): OrganizationId = OrganizationId(identifiers.get("organization").get)
  private def getEventId(identifiers: HashMap[String, String]): EventId = EventId(identifiers.get("event").get)
  private def getAttendeeId(identifiers: HashMap[String, String]): AttendeeId = AttendeeId(identifiers.get("attendee").get)
  private def getExponentId(identifiers: HashMap[String, String]): ExponentId = ExponentId(identifiers.get("exponent").get)
  private def getSessionId(identifiers: HashMap[String, String]): SessionId = SessionId(identifiers.get("session").get)
  private def getId(identifiers: HashMap[String, String], item: String): String = identifiers.get(item).get
}
