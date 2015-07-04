package common.models.user

import common.repositories.Repository
import java.util.UUID
import play.api.libs.json.Json
import com.mohiva.play.silhouette.core.Identity
import com.mohiva.play.silhouette.core.LoginInfo

case class UserInfo(
  firstName: String,
  lastName: String)
case class UserRights(
  global: Map[String, Boolean])
case class User(
  uuid: String = Repository.generateUuid(),
  loginInfo: LoginInfo,
  email: String,
  info: UserInfo,
  rights: UserRights = UserRights(Map())) extends Identity
object User {
  implicit val formatLoginInfo = Json.format[LoginInfo]
  implicit val formatUserInfo = Json.format[UserInfo]
  implicit val formatUserRights = Json.format[UserRights]
  implicit val format = Json.format[User]
}
