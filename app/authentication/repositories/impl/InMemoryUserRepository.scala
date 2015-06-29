package authentication.repositories.impl

import authentication.models.User
import authentication.repositories.UserRepository
import authentication.repositories.UserCreationException
import scala.collection.mutable
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.core.LoginInfo

class InMemoryUserRepository extends UserRepository {
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    play.Logger.info("find User for loginInfo: " + loginInfo)
    Future.successful(InMemoryUserRepository.users.find { case (id, user) => user.loginInfo == loginInfo }.map(_._2))
  }
  def save(user: User) = {
    play.Logger.info("save User: " + user)
    if (InMemoryUserRepository.users.contains(user.username)) {
      Future.failed(new UserCreationException("username already exists."))
    } else {
      InMemoryUserRepository.users += (user.username -> user)
      Future.successful(user)
    }
  }
}
object InMemoryUserRepository {
  private val users: mutable.HashMap[String, User] = mutable.HashMap()
}
