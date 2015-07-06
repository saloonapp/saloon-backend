package common.models.user

import common.repositories.Repository
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime

case class OrganizationMeta(
  created: DateTime,
  updated: DateTime)
case class Organization(
  uuid: String = Repository.generateUuid(),
  name: String,
  meta: OrganizationMeta = OrganizationMeta(new DateTime(), new DateTime()))
object Organization {
  implicit val formatOrganizationMeta = Json.format[OrganizationMeta]
  implicit val format = Json.format[Organization]
}

// mapping object for Organization Form
case class OrganizationData(
  name: String)
object OrganizationData {
  val fields = mapping(
    "name" -> nonEmptyText)(OrganizationData.apply)(OrganizationData.unapply)
  def toModel(d: OrganizationData): Organization = Organization(Repository.generateUuid(), d.name, OrganizationMeta(new DateTime(), new DateTime()))
  def fromModel(d: Organization): OrganizationData = OrganizationData(d.name)
  def merge(m: Organization, d: OrganizationData): Organization = toModel(d).copy(uuid = m.uuid, meta = OrganizationMeta(m.meta.created, new DateTime()))
}
