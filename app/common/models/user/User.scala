package common.models.user

import common.repositories.Repository
import java.util.UUID
import play.api.libs.json.Json
import com.mohiva.play.silhouette.core.Identity
import com.mohiva.play.silhouette.core.LoginInfo

case class UserRights(
  global: Map[String, Boolean])
case class User(
  uuid: String = Repository.generateUuid(),
  loginInfo: LoginInfo,
  email: String,
  rights: UserRights = UserRights(Map())) extends Identity
object User {
  implicit val formatLoginInfo = Json.format[LoginInfo]
  implicit val formatUserRights = Json.format[UserRights]
  implicit val format = Json.format[User]
}
