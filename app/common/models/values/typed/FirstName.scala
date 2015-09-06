package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class FirstName(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object FirstName extends tStringHelper[FirstName] {
  def build(str: String): Either[String, FirstName] = Right(FirstName(str))
}
