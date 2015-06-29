package authentication.repositories

import com.mohiva.play.silhouette.core.providers.PasswordInfo
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO

trait PasswordRepository extends DelegableAuthInfoDAO[PasswordInfo] {

}
