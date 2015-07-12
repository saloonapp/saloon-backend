package authentication.repositories.impl

import common.models.user.User
import authentication.repositories.UserRepository
import authentication.repositories.UserCreationException
import scala.collection.mutable
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.core.LoginInfo

class InMemoryUserRepository extends UserRepository {
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    Future.successful(InMemoryUserRepository.users.find { case (id, user) => user.loginInfo == loginInfo }.map(_._2))
  }
  def save(user: User): Future[User] = {
    if (InMemoryUserRepository.users.contains(user.email)) {
      Future.failed(new UserCreationException("email already exists."))
    } else {
      InMemoryUserRepository.users += (user.email -> user)
      Future.successful(user)
    }
  }
}
object InMemoryUserRepository {
  private val users: mutable.HashMap[String, User] = mutable.HashMap()
}
