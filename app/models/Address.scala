package models

import play.api.data.Forms._
import play.api.libs.json.Json

case class Address(
  name: String)
object Address {
  implicit val format = Json.format[Address]
  val fields = mapping(
    "name" -> text)(Address.apply)(Address.unapply)
}
