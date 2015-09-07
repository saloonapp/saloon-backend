package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class TextInline(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object TextInline extends tStringHelper[TextInline] {
  def build(str: String): Either[String, TextInline] = Right(TextInline(str))
}
