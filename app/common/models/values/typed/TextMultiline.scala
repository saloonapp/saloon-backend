package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class TextMultiline(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object TextMultiline extends tStringHelper[TextMultiline] {
  def build(str: String): Option[TextMultiline] = Some(TextMultiline(str))
}
