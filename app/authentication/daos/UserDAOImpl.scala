package authentication.daos

import scala.collection.mutable
import scala.concurrent.Future

import com.mohiva.play.silhouette.core.LoginInfo

import authentication.models.User
import authentication.services.UserCreationException
import UserDAOImpl._

class UserDAOImpl extends UserDAO {

  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    play.Logger.info("find User for loginInfo: " + loginInfo)
    Future.successful(users.find { case (id, user) => user.loginInfo == loginInfo }.map(_._2))
  }

  def find(username: String): Future[Option[User]] = {
    play.Logger.info("find User by username: " + username)
    Future.successful(users.get(username))
  }

  def save(user: User): Future[User] = {
    play.Logger.info("save User: " + user)
    if (users.contains(user.username)) {
      Future.failed(new UserCreationException("username already exists."))
    } else {
      users += (user.username -> user)
      Future.successful(user)
    }
  }
}
object UserDAOImpl {
  val users: mutable.HashMap[String, User] = mutable.HashMap()
}
