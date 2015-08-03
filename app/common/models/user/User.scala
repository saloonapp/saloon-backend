package common.models.user

import common.repositories.Repository
import java.util.UUID
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime
import com.mohiva.play.silhouette.core.Identity
import com.mohiva.play.silhouette.core.LoginInfo

case class UserOrganization(
  organizationId: String,
  role: String)
object UserOrganization {
  val owner = "owner"
  val admin = "admin"
  val member = "member"
  val guest = "guest"
  val all = List(owner, admin, member, guest)
  def getPriority(role: String): Int = getPriority(Some(role))
  def getPriority(roleOpt: Option[String]): Int = {
    roleOpt.map { role =>
      val index = all.indexOf(role)
      if (index == -1) all.length + 1 else index + 1
    }.getOrElse(all.length + 2)
  }
}
case class UserInfo(
  firstName: String,
  lastName: String)
case class UserRights(
  global: Map[String, Boolean])
case class UserMeta(
  created: DateTime,
  updated: DateTime)
case class User(
  uuid: String = Repository.generateUuid(),
  organizationIds: List[UserOrganization] = List(),
  loginInfo: LoginInfo,
  email: String,
  info: UserInfo,
  rights: Map[String, Boolean] = Map(),
  meta: UserMeta = UserMeta(new DateTime(), new DateTime())) extends Identity {
  def name(): String = this.info.firstName + " " + this.info.lastName
  def organizationRole(uuid: String): Option[String] = this.organizationIds.find(_.organizationId == uuid).map(_.role)
  def canAdministrateOrganization(uuid: String): Boolean = this.organizationRole(uuid).map(_ == UserOrganization.owner).getOrElse(false)
  def canAdministrateSaloon(): Boolean = hasRight(UserRight.administrateSalooN)
  def canCreateEvent(): Boolean = hasRight(UserRight.createEvent)
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
  val createEvent = UserRight("createEvent", "Créer un événement")

  val all = Seq(administrateSalooN, createEvent)
}

// mapping object for User Form
case class UserData(
  email: String,
  info: UserInfo,
  rights: List[String])
object UserData {
  val fields = mapping(
    "email" -> email,
    "info" -> mapping(
      "firstName" -> text,
      "lastName" -> text)(UserInfo.apply)(UserInfo.unapply),
    "rights" -> list(text))(UserData.apply)(UserData.unapply)
  val rights: Seq[(String, String)] = UserRight.all.map(r => (r.key, r.label))

  def toModel(d: UserData): User = User(Repository.generateUuid(), List(), LoginInfo("", ""), d.email, d.info, toRights(d.rights), UserMeta(new DateTime(), new DateTime()))
  def fromModel(d: User): UserData = UserData(d.email, d.info, fromRights(d.rights))
  def merge(m: User, d: UserData): User = toModel(d).copy(uuid = m.uuid, organizationIds = m.organizationIds, loginInfo = m.loginInfo, meta = UserMeta(m.meta.created, new DateTime()))

  private def toRights(rights: List[String]): Map[String, Boolean] = rights.map(r => (r, true)).toMap
  private def fromRights(rights: Map[String, Boolean]): List[String] = rights.map(_._1).toList
}
