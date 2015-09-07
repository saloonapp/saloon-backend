package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class FullName(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object FullName extends tStringHelper[FullName] {
  def build(str: String): Either[String, FullName] = Right(FullName(str))
  def build(firstName: FirstName, lastName: LastName): FullName = FullName(firstName.unwrap + " " + lastName.unwrap)
}
