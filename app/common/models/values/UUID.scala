package common.models.values

import common.models.utils.tString
import common.models.utils.tStringHelper

trait UUID extends Any with tString {
  def toGenericId: GenericId = GenericId(this.unwrap)
}
object UUID {
  private val identifier = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})".r
  def generate(): String = java.util.UUID.randomUUID().toString()
  def toUUID(str: String): Option[String] = str match {
    case identifier(id) => Some(id)
    case _ => None
  }
}
