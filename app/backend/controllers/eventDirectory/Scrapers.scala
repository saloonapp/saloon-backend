package backend.controllers.eventDirectory

import common.models.values.UUID
import common.models.event.GenericEvent
import common.repositories.event.GenericEventRepository
import backend.utils.WSUtils
import backend.models.ScrapersConfig
import backend.repositories.ConfigRepository
import backend.services.GenericEventImport
import backend.forms.ScraperData
import authentication.environments.SilhouetteEnvironment
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import reactivemongo.core.commands.LastError

object Scrapers extends SilhouetteEnvironment {
  val createForm: Form[ScraperData] = Form(ScraperData.fields)
  val refreshForm = Form(single(
    "data" -> nonEmptyText))

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    ConfigRepository.getScrapersConfig().map { scrapersConfigOpt =>
      val scraperConfig = scrapersConfigOpt.getOrElse(ScrapersConfig())
      Ok(backend.views.html.eventDirectory.Scrapers.list(scraperConfig))
    }
  }

  def create = SecuredAction { implicit req =>
    implicit val user = req.identity
    Ok(backend.views.html.eventDirectory.Scrapers.create(createForm))
  }

  def doCreate = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(backend.views.html.eventDirectory.Scrapers.create(formWithErrors))),
      formData => ConfigRepository.addScraper(ScraperData.toModel(formData)).map { err =>
        if (err.ok) {
          Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("success" -> s"Scraper '${formData.name}' créé !")
        } else {
          Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("error" -> s"Problème lors de la création du scraper '${formData.name}' :(")
        }
      })
  }

  def refresh(scraperId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    ConfigRepository.getScraper(scraperId).flatMap { scraperOpt =>
      scraperOpt.map { scraper =>
        WSUtils.fetch(scraper.url.unwrap).flatMap { resTry =>
          resTry.flatMap(res => Try(Json.parse(res).as[List[GenericEvent]].map(_.copy(uuid = UUID.generate())))) match {
            case Success(newEvents) => {
              val sourceName = newEvents.flatMap(_.sources).map(_.name).filter(_ != "").headOption.getOrElse("")
              GenericEventRepository.findBySourceName(sourceName).map { existingEvents =>
                val (createdEvents, deletedEvents, updatedEvents) = GenericEventImport.makeDiff(existingEvents, newEvents)
                Ok(backend.views.html.eventDirectory.Scrapers.refresh(refreshForm, scraper, newEvents, createdEvents, deletedEvents, updatedEvents))
              }
            }
            case Failure(e) => Future(Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("error" -> e.getMessage()))
          }
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("error" -> "Le scraper demandé n'existe pas !"))
      }
    }
  }

  def doRefresh(scraperId: String) = SecuredAction.async(parse.urlFormEncoded(maxLength = 1024 * 1000 * 10)) { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      refreshForm.bindFromRequest.fold(
        formWithErrors => Future(Redirect(backend.controllers.eventDirectory.routes.Scrapers.refresh(scraperId)).flashing("error" -> "Format de données incorrect...")),
        formData => {
          Try(Json.parse(formData).as[List[GenericEvent]]) match {
            case Success(newEvents) => {
              val sourceName = newEvents.flatMap(_.sources).map(_.name).filter(_ != "").headOption.getOrElse("")
              GenericEventRepository.findBySourceName(sourceName).flatMap { existingEvents =>
                val (createdEvents, deletedEvents, updatedEvents) = GenericEventImport.makeDiff(existingEvents, newEvents)
                val err = new LastError(true, None, None, None, None, 0, false)
                for {
                  scraperUpdated <- ConfigRepository.scraperExecuted(scraperId)
                  eventsCreated <- if (createdEvents.length > 0) { GenericEventRepository.bulkInsert(createdEvents) } else { Future(0) }
                  eventsDeleted <- if (deletedEvents.length > 0) { GenericEventRepository.bulkDelete(deletedEvents.map(_.uuid)) } else { Future(err) }
                  eventsUpdated <- if (updatedEvents.length > 0) { GenericEventRepository.bulkUpdate(updatedEvents.map(s => (s._2.uuid, s._2))) } else { Future(0) }
                } yield {
                  Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("success" -> s"Modifications des événements: $eventsCreated/$eventsUpdated/${deletedEvents.length} (create/update/delete)")
                }
              }
            }
            case Failure(e) => Future(Redirect(backend.controllers.eventDirectory.routes.Scrapers.refresh(scraperId)).flashing("error" -> e.getMessage()))
          }
        })
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def doDelete(scraperId: String) = SecuredAction.async { implicit req =>
    ConfigRepository.deleteScraper(scraperId).map { err =>
      if (err.ok) {
        Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("success" -> "Scraper supprimé !")
      } else {
        Redirect(backend.controllers.eventDirectory.routes.Scrapers.list).flashing("error" -> "Problème lors de la suppression du scraper :(")
      }
    }
  }

}
