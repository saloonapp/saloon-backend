package tools.utils

import play.api.libs.json._

trait CsvElt {
  def toCsv(): Map[String, String]
}
object CsvUtils {
  def jsonToCsv(json: JsValue, maxDeep: Int = 1): Map[String, String] = toCsv(json, maxDeep).toMap

  private def toCsv(json: JsValue, maxDeep: Int, prefix: String = ""): List[(String, String)] = {
    json match {
      case JsString(v) => List((prefix, v))
      case JsNumber(v) => List((prefix, v.toString))
      case JsBoolean(v) => List((prefix, v.toString))
      case _: JsUndefined => List()
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
    }
  }
}