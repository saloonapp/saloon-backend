package authentication.repositories

import scala.concurrent.Future
import com.mohiva.play.silhouette.core.LoginInfo
import com.mohiva.play.silhouette.core.providers.PasswordInfo
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO

trait PasswordRepository extends DelegableAuthInfoDAO[PasswordInfo] {
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]]
  def save(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo]
}
