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


object FileImporter {
  val datePattern = "dd/MM/yyyy HH:mm"
  val dateFormat = DateTimeFormat.forPattern(datePattern)
  val delimiter = ';'
  val eol = '\n'

  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = FileImporter.delimiter
  }

  def importEvents(importedFile: Reader, cfg: ImportConfig): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val elts = lines.map { line => Event.fromMap(line) }.flatten

    if (cfg.shouldClean) {
      EventRepository.drop().flatMap { dropped =>
        EventRepository.bulkInsert(elts)
      }
    } else {
      EventRepository.bulkInsert(elts)
    }
  }

  def importExponents(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val elts = lines.map { line => Exponent.fromMap(line, eventId) }.flatten

    if (cfg.shouldClean) {
      ExponentRepository.drop().flatMap { dropped =>
        ExponentRepository.bulkInsert(elts)
      }
    } else {
      ExponentRepository.bulkInsert(elts)
    }
  }

  def importSessions(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val elts = lines.map { line => Session.fromMap(line, eventId) }.flatten

    if (cfg.shouldClean) {
      SessionRepository.drop().flatMap { dropped =>
        SessionRepository.bulkInsert(elts)
      }
    } else {
      SessionRepository.bulkInsert(elts)
    }
  }
}
