package backend.views.partials

import play.api.mvc.Call
import scala.collection.mutable.MutableList
import scala.collection.mutable.HashMap

object Breadcrumb {
  def buildBreadcrumb(breadcrumb: String, titles: Map[String, String]): Option[(List[(Call, String)], String)] = {
    prepare(breadcrumb).map { list =>
      val result = new MutableList[(Call, String)]()
      val identifiers = new HashMap[String, String]()
      for (i <- 0 until list.length) {
        val prev = if (i > 0) Some(list(i - 1)) else None
        val prev2 = if (i > 1) Some(list(i - 2)) else None
        result += build(list(i), prev, prev2, titles, identifiers)
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
      case "myEvents" => (backend.controllers.routes.Events.list(), "Mes événements")
      case "attendees" => (backend.controllers.routes.Attendees.list(identifiers.get("event").get), "Participants")
      case "exponents" => (backend.controllers.routes.Exponents.list(identifiers.get("event").get), "Exposants")
      case "sessions" => (backend.controllers.routes.Sessions.list(identifiers.get("event").get), "Sessions")
      case "ticketing" => (backend.controllers.routes.Ticketing.details(identifiers.get("event").get), "Ticketing")
      case "create" => prev.get match {
        case "myEvents" => (backend.controllers.routes.Events.create(), "Créer un événement")
        case "attendees" => (backend.controllers.routes.Attendees.create(identifiers.get("event").get), "Nouveau participant")
        case "exponents" => (backend.controllers.routes.Exponents.create(identifiers.get("event").get), "Nouvel exposant")
        case "sessions" => (backend.controllers.routes.Sessions.create(identifiers.get("event").get), "Nouvelle session")
      }
      case "edit" => prev.get match {
        case "profile" => (backend.controllers.routes.Profile.details, "Modification")
        case _ =>
          prev2.get match {
            case "organizations" => (backend.controllers.routes.Organizations.update(identifiers.get("organization").get), "Modification")
            case "myEvents" => (backend.controllers.routes.Events.update(identifiers.get("event").get), "Modification")
            case "attendees" => (backend.controllers.routes.Attendees.update(identifiers.get("event").get, identifiers.get("attendee").get), "Modification")
            case "exponents" => (backend.controllers.routes.Exponents.update(identifiers.get("event").get, identifiers.get("exponent").get), "Modification")
            case "sessions" => (backend.controllers.routes.Sessions.update(identifiers.get("event").get, identifiers.get("session").get), "Modification")
          }
      }
      case "delete" => prev2.get match {
        case "organizations" => (backend.controllers.routes.Organizations.delete(identifiers.get("organization").get), "Suppression")
      }
      case "config" => prev.get match {
        case "ticketing" => (backend.controllers.routes.Ticketing.configure(identifiers.get("event").get), "Configuration")
      }
      case identifier(id) => prev.get match {
        case "organizations" => {
          identifiers.put("organization", id)
          (backend.controllers.routes.Organizations.details(id), titles.get(id).get)
        }
        case "myEvents" => {
          identifiers.put("event", id)
          (backend.controllers.routes.Events.details(id), titles.get(id).get)
        }
        case "attendees" => {
          identifiers.put("attendee", id)
          (backend.controllers.routes.Attendees.details(identifiers.get("event").get, id), titles.get(id).get)
        }
        case "exponents" => {
          identifiers.put("exponent", id)
          (backend.controllers.routes.Exponents.details(identifiers.get("event").get, id), titles.get(id).get)
        }
        case "sessions" => {
          identifiers.put("session", id)
          (backend.controllers.routes.Sessions.details(identifiers.get("event").get, id), titles.get(id).get)
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
}
