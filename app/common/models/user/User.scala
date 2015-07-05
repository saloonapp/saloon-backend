package common.models.user

import common.repositories.Repository
import java.util.UUID
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime
import com.mohiva.play.silhouette.core.Identity
import com.mohiva.play.silhouette.core.LoginInfo

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
  loginInfo: LoginInfo,
  email: String,
  info: UserInfo,
  rights: Map[String, Boolean] = Map(),
  meta: UserMeta = UserMeta(new DateTime(), new DateTime())) extends Identity
object User {
  implicit val formatLoginInfo = Json.format[LoginInfo]
  implicit val formatUserInfo = Json.format[UserInfo]
  implicit val formatUserRights = Json.format[UserRights]
  implicit val formatUserMeta = Json.format[UserMeta]
  implicit val format = Json.format[User]
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
  val rights: Seq[(String, String)] = Seq(("admin-saloon", "Administrer SalooN"), ("create-event", "Créer un événement"))

  def toModel(d: UserData): User = User(Repository.generateUuid(), LoginInfo("", ""), d.email, d.info, toRights(d.rights), UserMeta(new DateTime(), new DateTime()))
  def fromModel(d: User): UserData = UserData(d.email, d.info, fromRights(d.rights))
  def merge(m: User, d: UserData): User = toModel(d).copy(uuid = m.uuid, loginInfo = m.loginInfo, meta = UserMeta(m.meta.created, new DateTime()))

  private def toRights(rights: List[String]): Map[String, Boolean] = rights.map(r => (r, true)).toMap
  private def fromRights(rights: Map[String, Boolean]): List[String] = rights.map(_._1).toList
}
