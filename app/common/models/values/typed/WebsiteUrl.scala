package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class WebsiteUrl(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object WebsiteUrl extends tStringHelper[WebsiteUrl] {
  def build(str: String): Option[WebsiteUrl] = Some(WebsiteUrl(str)) // TODO : add validation
}
