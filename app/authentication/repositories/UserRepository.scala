package authentication.repositories

import common.models.user.User
import scala.concurrent.Future
import com.mohiva.play.silhouette.core.services.IdentityService
import com.mohiva.play.silhouette.core.LoginInfo

trait UserRepository extends IdentityService[User] {
  def retrieve(loginInfo: LoginInfo): Future[Option[User]]
  def save(user: User): Future[User]
}

case class UserCreationException(msg: String, cause: Throwable) extends Exception(msg, cause) {
  def this(msg: String) = this(msg, null)
}
