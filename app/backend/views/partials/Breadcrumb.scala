package backend.views.partials

import common.models.values.UUID
import common.models.user.OrganizationId
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.ExponentId
import common.models.event.SessionId
import play.api.mvc.Call
import scala.collection.mutable.MutableList
import scala.collection.mutable.HashMap

object Breadcrumb {
  def buildBreadcrumb(breadcrumb: String, titles: Map[UUID, String]): Option[(List[(Call, String)], String)] = {
    prepare(breadcrumb).map { list =>
      val result = new MutableList[(Call, String)]()
      val identifiers = new HashMap[String, String]()
      for (i <- 0 until list.length) {
        val prev = if (i > 0) Some(list(i - 1)) else None
        val prev2 = if (i > 1) Some(list(i - 2)) else None
        result += build(list(i), prev, prev2, titles.map { case (key, value) => (key.unwrap, value) }, identifiers)
      }
      (result.toList.take(result.length - 1), result.last._2)
    }
  }

  private def prepare(breadcrumb: String): Option[List[String]] = {
    val list = breadcrumb.split("_").toList
    if (breadcrumb != "" && list.length > 0 && !breadcrumb.startsWith("welcome")) {
      if (breadcrumb.startsWith("home")) {
        Some(list)
      } else {
        Some(List("home") ++ list)
      }
    } else {
      None
    }
  }

  private val identifier = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})".r
  private def build(current: String, prev: Option[String], prev2: Option[String], titles: Map[String, String], identifiers: HashMap[String, String]): (Call, String) = {
    current match {
      case "home" => (backend.controllers.routes.Application.index(), "Accueil")
      case "profile" => (backend.controllers.routes.Profile.details(), "Profil")
      case "organizations" => (backend.controllers.routes.Profile.details(), "Organisations")
      case "events" => (backend.controllers.routes.Events.list(), "Mes événements")
      case "attendees" => (backend.controllers.routes.Attendees.list(getEventId(identifiers)), "Participants")
      case "exponents" => (backend.controllers.routes.Exponents.list(getEventId(identifiers)), "Exposants")
      case "team" => (backend.controllers.routes.Exponents.details(getEventId(identifiers), getExponentId(identifiers)), "Équipe")
      case "sessions" => (backend.controllers.routes.Sessions.list(getEventId(identifiers)), "Sessions")
      case "ticketing" => (backend.controllers.routes.Ticketing.details(getEventId(identifiers)), "Ticketing")
      case "create" => prev.get match {
        case "events" => (backend.controllers.routes.Events.create(), "Créer un événement")
        case "attendees" => (backend.controllers.routes.Attendees.create(getEventId(identifiers)), "Nouveau participant")
        case "exponents" => (backend.controllers.routes.Exponents.create(getEventId(identifiers)), "Nouvel exposant")
        case "team" => (backend.controllers.routes.Exponents.teamCreate(getEventId(identifiers), getExponentId(identifiers)), "Nouveau membre")
        case "sessions" => (backend.controllers.routes.Sessions.create(getEventId(identifiers)), "Nouvelle session")
      }
      case "edit" => prev.get match {
        case "profile" => (backend.controllers.routes.Profile.details, "Modification")
        case _ =>
          prev2.get match {
            case "organizations" => (backend.controllers.routes.Organizations.update(getOrganizationId(identifiers)), "Modification")
            case "events" => (backend.controllers.routes.Events.update(getEventId(identifiers)), "Modification")
            case "attendees" => (backend.controllers.routes.Attendees.update(getEventId(identifiers), getAttendeeId(identifiers)), "Modification")
            case "exponents" => (backend.controllers.routes.Exponents.update(getEventId(identifiers), getExponentId(identifiers)), "Modification")
            case "team" => (backend.controllers.routes.Exponents.teamUpdate(getEventId(identifiers), getExponentId(identifiers), getAttendeeId(identifiers)), "Modification")
            case "sessions" => (backend.controllers.routes.Sessions.update(getEventId(identifiers), getSessionId(identifiers)), "Modification")
          }
      }
      case "delete" => prev2.get match {
        case "organizations" => (backend.controllers.routes.Organizations.delete(getOrganizationId(identifiers)), "Suppression")
      }
      case "config" => prev.get match {
        case "ticketing" => (backend.controllers.routes.Ticketing.configure(getEventId(identifiers)), "Configuration")
      }
      case identifier(id) => {
        setId(identifiers, prev.get, id)
        prev.get match {
          case "organizations" => (backend.controllers.routes.Organizations.details(getOrganizationId(identifiers)), titles.get(id).get)
          case "events" => (backend.controllers.routes.Events.details(getEventId(identifiers)), titles.get(id).get)
          case "attendees" => (backend.controllers.routes.Attendees.details(getEventId(identifiers), getAttendeeId(identifiers)), titles.get(id).get)
          case "exponents" => (backend.controllers.routes.Exponents.details(getEventId(identifiers), getExponentId(identifiers)), titles.get(id).get)
          case "team" => (backend.controllers.routes.Exponents.teamDetails(getEventId(identifiers), getExponentId(identifiers), getAttendeeId(identifiers)), titles.get(id).get)
          case "sessions" => (backend.controllers.routes.Sessions.details(getEventId(identifiers), getSessionId(identifiers)), titles.get(id).get)
        }
      }

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
    case "events" => identifiers.put("event", id)
    case "attendees" => identifiers.put("attendee", id)
    case "exponents" => identifiers.put("exponent", id)
    case "team" => identifiers.put("attendee", id)
    case "sessions" => identifiers.put("session", id)
  }
  private def getOrganizationId(identifiers: HashMap[String, String]): OrganizationId = OrganizationId(identifiers.get("organization").get)
  private def getEventId(identifiers: HashMap[String, String]): EventId = EventId(identifiers.get("event").get)
  private def getAttendeeId(identifiers: HashMap[String, String]): AttendeeId = AttendeeId(identifiers.get("attendee").get)
  private def getExponentId(identifiers: HashMap[String, String]): ExponentId = ExponentId(identifiers.get("exponent").get)
  private def getSessionId(identifiers: HashMap[String, String]): SessionId = SessionId(identifiers.get("session").get)
}
