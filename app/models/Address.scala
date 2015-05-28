package models

import play.api.data.Forms._
import play.api.libs.json.Json

case class Address(
  name: String, // name of place, ex: La Grande Crypte
  street: String, // ex: 69bis Rue Boissière
  zipCode: String, // ex: 75116
  city: String) // ex: Paris
object Address {
  implicit val format = Json.format[Address]
  val fields = mapping(
    "name" -> text,
    "street" -> text,
    "zipCode" -> text,
    "city" -> text)(Address.apply)(Address.unapply)
}
