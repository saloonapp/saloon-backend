package common.models.user

import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import common.models.values.typed._
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime
import com.mohiva.play.silhouette.core.Identity
import com.mohiva.play.silhouette.core.LoginInfo

case class UserId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object UserId extends tStringHelper[UserId] {
  def generate(): UserId = UserId(UUID.generate())
  def build(str: String): Either[String, UserId] = UUID.toUUID(str).right.map(id => UserId(id)).left.map(_ + " for UserId")
}

case class UserOrganization(
  organizationId: OrganizationId,
  role: UserRole)
case class UserInfo(
  firstName: FirstName,
  lastName: LastName)
case class UserRights(
  global: Map[String, Boolean])
case class UserMeta(
  created: DateTime,
  updated: DateTime)
case class User(
  uuid: UserId = UserId.generate(),
  organizationIds: List[UserOrganization] = List(),
  loginInfo: LoginInfo,
  email: Email,
  info: UserInfo,
  rights: Map[String, Boolean] = Map(),
  meta: UserMeta = UserMeta(new DateTime(), new DateTime())) extends Identity {
  def name(): String = this.info.firstName.unwrap + " " + this.info.lastName.unwrap
  def organizationRole(uuid: OrganizationId): Option[UserRole] = this.organizationIds.find(_.organizationId == uuid).map(_.role)
  def canAdministrateOrganization(uuid: OrganizationId): Boolean = this.organizationRole(uuid).map(_ == UserRole.owner).getOrElse(false)
  def canAdministrateSaloon(): Boolean = hasRight(UserRight.administrateSalooN)
  def hasRight(right: UserRight): Boolean = this.rights.get(right.key).getOrElse(false)
}
object User {
  implicit val formatUserOrganization = Json.format[UserOrganization]
  implicit val formatLoginInfo = Json.format[LoginInfo]
  implicit val formatUserInfo = Json.format[UserInfo]
  implicit val formatUserRights = Json.format[UserRights]
  implicit val formatUserMeta = Json.format[UserMeta]
  implicit val format = Json.format[User]
}

case class UserRight(key: String, label: String)
object UserRight {
  val administrateSalooN = UserRight("administrateSaloon", "Administrer SalooN")
  val all = Seq(administrateSalooN)
}

// mapping object for User Form
case class UserData(
  email: Email,
  info: UserInfo,
  rights: List[String])
object UserData {
  val fields = mapping(
    "email" -> of[Email],
    "info" -> mapping(
      "firstName" -> of[FirstName],
      "lastName" -> of[LastName])(UserInfo.apply)(UserInfo.unapply),
    "rights" -> list(text))(UserData.apply)(UserData.unapply)
  val rights: Seq[(String, String)] = UserRight.all.map(r => (r.key, r.label))

  def toModel(d: UserData): User = User(UserId.generate(), List(), LoginInfo("", ""), d.email, d.info, toRights(d.rights), UserMeta(new DateTime(), new DateTime()))
  def fromModel(d: User): UserData = UserData(d.email, d.info, fromRights(d.rights))
  def merge(m: User, d: UserData): User = toModel(d).copy(uuid = m.uuid, organizationIds = m.organizationIds, loginInfo = m.loginInfo, meta = UserMeta(m.meta.created, new DateTime()))

  private def toRights(rights: List[String]): Map[String, Boolean] = rights.map(r => (r, true)).toMap
  private def fromRights(rights: Map[String, Boolean]): List[String] = rights.map(_._1).toList
}
