package services

import models.ImportConfig
import models.Exponent
import infrastructure.repository.ExponentRepository
import java.io.Reader
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.github.tototoshi.csv._


object FileImporter {
  val delimiter = ';'
  val eol = '\n'

  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = FileImporter.delimiter
  }

  /*def importExponents(importedFile: File, shouldClean: Boolean, eventId: String): Future[Int] = {
    val reader = CSVReader.open(importedFile)
    val lines = reader.allWithHeaders()
    val exponents = lines.map { line => Exponent.fromMap(line, eventId) }.flatten

    if (shouldClean) {
      ExponentRepository.drop().flatMap { dropped =>
        ExponentRepository.bulkInsert(exponents)
      }
    } else {
      ExponentRepository.bulkInsert(exponents)
    }
  }*/

  def importExponents(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val exponents = lines.map { line => Exponent.fromMap(line, eventId) }.flatten

    if (cfg.shouldClean) {
      ExponentRepository.drop().flatMap { dropped =>
        ExponentRepository.bulkInsert(exponents)
      }
    } else {
      ExponentRepository.bulkInsert(exponents)
    }
  }
}
