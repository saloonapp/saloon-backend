package backend.forms

import common.models.values.typed.FullName
import common.models.user.Organization
import common.models.user.OrganizationId
import play.api.data.Forms._
import org.joda.time.DateTime

case class OrganizationData(
  name: FullName)
object OrganizationData {
  val fields = mapping(
    "name" -> of[FullName])(OrganizationData.apply)(OrganizationData.unapply)

  def toModel(d: OrganizationData): Organization = Organization(OrganizationId.generate(), d.name)
  def fromModel(d: Organization): OrganizationData = OrganizationData(d.name)
  def merge(m: Organization, d: OrganizationData): Organization = m.copy(name = d.name, meta = m.meta.copy(updated = new DateTime()))
}