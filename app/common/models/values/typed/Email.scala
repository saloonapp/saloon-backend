package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class Email(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object Email extends tStringHelper[Email] {
  def build(str: String): Either[String, Email] = Right(Email(str)) // TODO : add validation
}
