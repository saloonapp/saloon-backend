package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class Genre(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object Genre extends tStringHelper[Genre] {
  def build(str: String): Either[String, Genre] = Right(Genre(str)) // TODO : add validation
}
