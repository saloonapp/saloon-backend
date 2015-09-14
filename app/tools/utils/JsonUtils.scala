package tools.utils

import play.api.libs.json._

object JsonUtils {

  def diff(a: JsValue, b: JsValue): JsObject = diff(a.as[JsObject], b.as[JsObject])
  def diff(a: JsObject, b: JsObject): JsObject = {
    val res = a.keys.map { key =>
      val aSub = a \ key
      val bSub = b \ key
      if (aSub == bSub) {
        None
      } else {
        bSub match {
          case JsArray(seq: Seq[JsValue]) => {
            val arr = seq.zipWithIndex.map {
              case (v, i) =>
                if (aSub(i) == bSub(i)) {
                  None
                } else {
                  Some(diff(aSub(i), bSub(i)))
                }
            }
            Some((key, Json.toJson(arr.flatten.toList)))
          }
          case obj: JsObject => Some((key, diff(aSub, bSub)))
          case _ => Some((key, bSub))
        }
      }
    }.flatten.toMap
    Json.toJson(res).as[JsObject]
  }

}