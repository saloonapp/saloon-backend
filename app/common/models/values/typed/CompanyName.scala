package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class CompanyName(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object CompanyName extends tStringHelper[CompanyName] {
  def build(str: String): Option[CompanyName] = Some(CompanyName(str))
}
