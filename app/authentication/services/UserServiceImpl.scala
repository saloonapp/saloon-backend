package authentication.services

import authentication.models.User
import authentication.daos.UserDAOImpl
import java.util.UUID
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.core.LoginInfo
import com.mohiva.play.silhouette.core.providers.CommonSocialProfile
import com.mohiva.play.silhouette.core.services.AuthInfo

class UserServiceImpl extends UserService {
  lazy val userDAO = new UserDAOImpl
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)
  def save(user: User) = userDAO.save(user)
}
