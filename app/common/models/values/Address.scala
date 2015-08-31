package common.models.values

import play.api.data.Forms._
import play.api.libs.json.Json

case class Address(
  name: String, // name of place, ex: La Grande Crypte
  street: String, // ex: 69bis Rue Boissière
  zipCode: String, // ex: 75116
  city: String) { // ex: Paris
  def isDefined(): Boolean = !(this.name.isEmpty && this.street.isEmpty)
  def map[T](f: Address => T): Option[T] = if (this.isDefined()) Some(f(this)) else None
}
object Address {
  implicit val format = Json.format[Address]
  val fields = mapping(
    "name" -> text,
    "street" -> text,
    "zipCode" -> text,
    "city" -> text)(Address.apply)(Address.unapply)
}
