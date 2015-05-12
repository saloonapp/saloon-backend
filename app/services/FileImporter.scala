package services

import models.ImportConfig
import models.Event
import models.Session
import models.Exponent
import infrastructure.repository.EventRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.SessionRepository
import org.joda.time.format.DateTimeFormat
import java.io.Reader
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.github.tototoshi.csv._
import common.Defaults.csvFormat

object FileImporter {
  val datePattern = "dd/MM/yyyy HH:mm"
  val dateFormat = DateTimeFormat.forPattern(datePattern)

  def importEvents(importedFile: Reader, cfg: ImportConfig): Future[(Int, List[Map[String, String]])] = {
    importData(Event.fromMap, EventRepository.drop, EventRepository.bulkInsert)(importedFile, cfg)
  }

  def importExponents(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[(Int, List[Map[String, String]])] = {
    importData(Exponent.fromMap(eventId), () => ExponentRepository.deleteByEvent(eventId).map(_.ok), ExponentRepository.bulkInsert)(importedFile, cfg)
  }

  def importSessions(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[(Int, List[Map[String, String]])] = {
    importData(Session.fromMap(eventId), () => SessionRepository.deleteByEvent(eventId).map(_.ok), SessionRepository.bulkInsert)(importedFile, cfg)
  }

  private def importData[T](formatData: Map[String, String] => Option[T], deleteOld: () => Future[Boolean], bulkInsert: List[T] => Future[Int])(importedFile: Reader, cfg: ImportConfig): Future[(Int, List[Map[String, String]])] = {
    val lines = CSVReader.open(importedFile).allWithHeaders().map { _.map { case (key, value) => (key, value.replace("\\r", "\r").replace("\\n", "\n")) } }
    val elts = lines.map { line => formatData(line) }.flatten
    val errors = lines.map { line => formatData(line).map(_ => None).getOrElse(Some(line)) }.flatten

    (if (cfg.shouldClean) { deleteOld() } else { Future(true) }).flatMap { _ =>
      bulkInsert(elts).map { inserted => (inserted, errors) }
    }
  }
}
