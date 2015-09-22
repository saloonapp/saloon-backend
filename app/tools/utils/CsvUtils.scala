package tools.utils

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.http.HeaderNames.CONTENT_DISPOSITION
import com.github.tototoshi.csv.CSVWriter
import com.github.tototoshi.csv.DefaultCSVFormat

object CsvUtils {
  def OkCsv(data: List[Map[String, String]], filename: String): Result = Ok(makeCsv(data)).withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\"")).as("text/csv")

  def jsonToCsv(json: JsValue, maxDeep: Int = 1): Map[String, String] = toCsv(json, maxDeep).toMap

  private def toCsv(json: JsValue, maxDeep: Int, prefix: String = ""): List[(String, String)] = {
    json match {
      case JsString(v) => List((prefix, v))
      case JsNumber(v) => List((prefix, v.toString))
      case JsBoolean(v) => List((prefix, v.toString))
      case JsArray(seq: Seq[JsValue]) => if (maxDeep > 0) {
        seq.toList.zipWithIndex.flatMap { case (v, i) => toCsv(v, maxDeep - 1, if (prefix.isEmpty) { i.toString } else { s"$prefix[$i]" }) }
      } else {
        List((prefix, Json.stringify(Json.toJson(seq))))
      }
      case obj: JsObject => if (maxDeep > 0) {
        obj.as[Map[String, JsValue]].toList.flatMap { case (key, value) => toCsv(value, maxDeep - 1, if (prefix.isEmpty) { key } else { s"$prefix.$key" }) }
      } else {
        List((prefix, Json.stringify(Json.toJson(obj))))
      }
      case _ => List()
    }
  }

  implicit object csvFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }

  def makeCsv(elts: List[Map[String, String]]): String = {
    if (elts.isEmpty) {
      "No elts to serialize..."
    } else {
      val headers = elts.flatMap(_.map(_._1)).distinct.sorted
      val writer = new java.io.StringWriter()
      val csvWriter = CSVWriter.open(writer)
      csvWriter.writeRow(headers)
      csvWriter.writeAll(elts.map { row => headers.map(header => row.get(header).getOrElse("")).map(csvCellFormat) })
      csvWriter.close()
      writer.toString()
    }
  }
  private def csvCellFormat(value: String): String = if (value != null) { value.replace("\r", "\\r").replace("\n", "\\n") } else { "" }
}
