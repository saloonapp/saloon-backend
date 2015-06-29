package authentication.models

import java.util.UUID
import play.api.libs.json.Json
import com.mohiva.play.silhouette.core.Identity
import com.mohiva.play.silhouette.core.LoginInfo

/**
 * The user object.
 *
 * @param userID The unique ID of the user.
 * @param loginInfo The linked login info.
 * @param username The username of the authenticated user.
 * @param email The email of the authenticated provider.
 */
case class User(
  loginInfo: LoginInfo,
  username: String,
  email: String) extends Identity
object User {
  implicit val formatLoginInfo = Json.format[LoginInfo]
  implicit val format = Json.format[User]
}
