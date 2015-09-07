package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class Color(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object Color extends tStringHelper[Color] {
  def build(str: String): Either[String, Color] = Right(Color(str)) // TODO : add validation
}
