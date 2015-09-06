package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class PhoneNumber(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object PhoneNumber extends tStringHelper[PhoneNumber] {
  def build(str: String): Either[String, PhoneNumber] = Right(PhoneNumber(str)) // TODO : add validation
}
