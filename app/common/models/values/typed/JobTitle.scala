package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class JobTitle(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object JobTitle extends tStringHelper[JobTitle] {
  def build(str: String): Option[JobTitle] = Some(JobTitle(str))
}
