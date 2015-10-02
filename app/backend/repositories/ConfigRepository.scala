package backend.repositories

import common.repositories.CollectionReferences
import backend.models.ScrapersConfig
import backend.models.Scraper
import backend.models.ScraperResult
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.JsObjectDocumentWriter
import play.modules.reactivemongo.json.collection.JSONCollection
import org.joda.time.DateTime

object ConfigRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.CONFIGS)

  private val scraperConfigSelector = Json.obj("scrapersConfig" -> true)
  def getScrapersConfig(): Future[Option[ScrapersConfig]] = collection.find(scraperConfigSelector).one[ScrapersConfig]
  def getScraper(scraperId: String): Future[Option[Scraper]] = getScrapersConfig().map { _.flatMap(_.scrapers.find(_.uuid == scraperId)) }
  def addScraper(scraper: Scraper): Future[WriteResult] = getScrapersConfig().flatMap { configOpt =>
    configOpt.map { config =>
      collection.update(scraperConfigSelector, Json.obj("$addToSet" -> Json.obj("scrapers" -> Json.toJson(scraper))))
    }.getOrElse {
      collection.insert(ScrapersConfig(List(scraper)))
    }
  }
  def updateScraper(scraperId: String, scraper: Scraper): Future[WriteResult] = collection.update(Json.obj("scrapersConfig" -> true, "scrapers.uuid" -> scraperId), Json.obj("$set" -> Json.obj("scrapers.$" -> scraper)))
  def scraperExecuted(scraperId: String, nbElts: Int): Future[WriteResult] = collection.update(Json.obj("scrapersConfig" -> true, "scrapers.uuid" -> scraperId), Json.obj("$set" -> Json.obj("scrapers.$.lastExec" -> ScraperResult(new DateTime(), nbElts))))
  def deleteScraper(scraperId: String): Future[WriteResult] = collection.update(scraperConfigSelector, Json.obj("$pull" -> Json.obj("scrapers" -> Json.obj("uuid" -> scraperId))))
}