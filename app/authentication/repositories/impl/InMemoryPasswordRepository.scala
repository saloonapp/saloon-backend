package authentication.repositories.impl

import authentication.repositories.PasswordRepository
import scala.collection.mutable
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mohiva.play.silhouette.core.LoginInfo
import com.mohiva.play.silhouette.core.providers.PasswordInfo

class InMemoryPasswordRepository extends PasswordRepository {
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    Future.successful(InMemoryPasswordRepository.data.get(loginInfo))
  }
  def save(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] = {
    InMemoryPasswordRepository.data += (loginInfo -> passwordInfo)
    Future.successful(passwordInfo)
  }
}
object InMemoryPasswordRepository {
  private val data: mutable.HashMap[LoginInfo, PasswordInfo] = mutable.HashMap()
}
