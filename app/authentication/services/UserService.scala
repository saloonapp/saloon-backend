package authentication.services

import authentication.models.User
import scala.concurrent.Future
import com.mohiva.play.silhouette.core.providers.CommonSocialProfile
import com.mohiva.play.silhouette.core.services.AuthInfo
import com.mohiva.play.silhouette.core.services.IdentityService

trait UserService extends IdentityService[User] {
  def save(user: User): Future[User]
}

/**
 * An exception thrown when the user cannot be created.
 */
case class UserCreationException(msg: String, cause: Throwable) extends Exception(msg, cause) {
  def this(msg: String) = this(msg, null)
}
