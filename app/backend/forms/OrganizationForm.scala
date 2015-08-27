package backend.forms

import common.models.user.Organization
import common.repositories.Repository
import play.api.data.Forms._
import org.joda.time.DateTime

case class OrganizationData(
  name: String)
object OrganizationData {
  val fields = mapping(
    "name" -> nonEmptyText)(OrganizationData.apply)(OrganizationData.unapply)

  def toModel(d: OrganizationData): Organization = Organization(Repository.generateUuid(), d.name)
  def fromModel(d: Organization): OrganizationData = OrganizationData(d.name)
  def merge(m: Organization, d: OrganizationData): Organization = m.copy(name = d.name, meta = m.meta.copy(updated = new DateTime()))
}