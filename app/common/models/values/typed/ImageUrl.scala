package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class ImageUrl(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object ImageUrl extends tStringHelper[ImageUrl] {
  def build(str: String): Either[String, ImageUrl] = Right(ImageUrl(str)) // TODO : add validation
}
