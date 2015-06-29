package authentication.daos

import com.mohiva.play.silhouette.core.LoginInfo
import com.mohiva.play.silhouette.core.providers.PasswordInfo
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO
import scala.collection.mutable
import scala.concurrent.Future
import PasswordInfoDAO._

class PasswordInfoDAO extends DelegableAuthInfoDAO[PasswordInfo] {

  /**
   * Saves the password info.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The password info to save.
   * @return The saved password info or None if the password info couldn't be saved.
   */
  def save(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] = {
    play.Logger.info("save PasswordInfo: " + passwordInfo + " for " + loginInfo)
    data += (loginInfo -> passwordInfo)
    Future.successful(passwordInfo)
  }

  /**
   * Finds the password info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved password info or None if no password info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    play.Logger.info("find PasswordInfo for LoginInfo: " + loginInfo)
    Future.successful(data.get(loginInfo))
  }
}

object PasswordInfoDAO {
  var data: mutable.HashMap[LoginInfo, PasswordInfo] = mutable.HashMap()
}
