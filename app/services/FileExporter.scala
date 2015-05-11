package services

import com.github.tototoshi.csv._
import common.Defaults.csvFormat

object FileExporter {
  def makeCsv(elts: List[Map[String, String]]): String = {
    if (elts.isEmpty) {
      ""
    } else {
      val writer = new java.io.StringWriter()
      val csvWriter = CSVWriter.open(writer)
      csvWriter.writeRow(elts.head.map { case (key, value) => key }.toList)
      csvWriter.writeAll(elts.map { _.map { case (key, value) => value.replace("\r", "\\r").replace("\n", "\\n") }.toList })
      csvWriter.close()
      writer.toString()
    }
  }
}
