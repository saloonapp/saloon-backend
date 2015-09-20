package backend.controllers.eventDirectory

import backend.models.ScrapersConfig
import backend.repositories.ConfigRepository
import backend.forms.ScraperData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.data.Form

object Scrapers extends SilhouetteEnvironment {
  val createForm: Form[ScraperData] = Form(ScraperData.fields)

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
