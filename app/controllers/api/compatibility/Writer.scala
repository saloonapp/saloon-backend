package controllers.api.compatibility

import models._
import play.api.libs.json._

object Writer {
  def write(data: Event): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Event.className)
  def write(data: Session): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Session.className)
  def write(data: Exponent): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Exponent.className)

  def write(data: (Event, Int, Int)): JsObject = write(data._1) ++ Json.obj("sessionCount" -> data._2, "exponentCount" -> data._3)
}