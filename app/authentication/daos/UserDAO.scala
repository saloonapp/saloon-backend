package authentication.daos

import java.util.UUID

import scala.concurrent.Future

import com.mohiva.play.silhouette.core.LoginInfo

import authentication.models.User

/**
 * Give access to the user object.
 */
trait UserDAO {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]]

  /**
   * Finds a user by its username.
   *
   * @param username The username of the user to find.
   * @return The found user or None if no user for the given username could be found.
   */
  def find(username: String): Future[Option[User]]

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[User]
}
