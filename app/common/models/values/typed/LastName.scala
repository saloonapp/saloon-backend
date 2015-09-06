package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class LastName(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object LastName extends tStringHelper[LastName] {
  def build(str: String): Either[String, LastName] = Right(LastName(str))
}
