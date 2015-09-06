package common.models.user

import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import common.models.values.typed._
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime

case class OrganizationId(val id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object OrganizationId extends tStringHelper[OrganizationId] {
  def generate(): OrganizationId = OrganizationId(UUID.generate())
  def build(str: String): Either[String, OrganizationId] = UUID.toUUID(str).right.map(id => OrganizationId(id)).left.map(_ + " for OrganizationId")
}

case class OrganizationMeta(
  created: DateTime,
  updated: DateTime)
case class Organization(
  uuid: OrganizationId = OrganizationId.generate(),
  name: FullName,
  meta: OrganizationMeta = OrganizationMeta(new DateTime(), new DateTime()))
object Organization {
  implicit val formatOrganizationMeta = Json.format[OrganizationMeta]
  implicit val format = Json.format[Organization]
}

// mapping object for Organization Form
case class OrganizationData(
  name: FullName)
object OrganizationData {
  val fields = mapping(
    "name" -> of[FullName])(OrganizationData.apply)(OrganizationData.unapply)
  def toModel(d: OrganizationData): Organization = Organization(OrganizationId.generate(), d.name, OrganizationMeta(new DateTime(), new DateTime()))
  def fromModel(d: Organization): OrganizationData = OrganizationData(d.name)
  def merge(m: Organization, d: OrganizationData): Organization = toModel(d).copy(uuid = m.uuid, meta = OrganizationMeta(m.meta.created, new DateTime()))
}
