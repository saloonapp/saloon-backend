package backend.repositories

import common.repositories.CollectionReferences
import backend.models.ScrapersConfig
import backend.models.Scraper
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import org.joda.time.DateTime

object ConfigRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.CONFIGS)

  private val scraperConfigSelector = Json.obj("scrapersConfig" -> true)
  def getScrapersConfig(): Future[Option[ScrapersConfig]] = collection.find(scraperConfigSelector).one[ScrapersConfig]
  def addScraper(scraper: Scraper): Future[LastError] = getScrapersConfig().flatMap { configOpt =>
    configOpt.map { config =>
      collection.update(scraperConfigSelector, Json.obj("$addToSet" -> Json.obj("scrapers" -> Json.toJson(scraper))))
    }.getOrElse {
      collection.insert(ScrapersConfig(List(scraper)))
    }
  }
  def deleteScraper(scraperId: String): Future[LastError] = collection.update(scraperConfigSelector, Json.obj("$pull" -> Json.obj("scrapers" -> Json.obj("uuid" -> scraperId))))
}