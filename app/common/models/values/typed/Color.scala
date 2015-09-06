package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class Color(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object Color extends tStringHelper[Color] {
  def build(str: String): Option[Color] = Some(Color(str)) // TODO : add validation
}
