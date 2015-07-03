package authentication.environments

import authentication.repositories.UserRepository
import authentication.repositories.impl.MongoUserRepository
import authentication.repositories.impl.MongoPasswordRepository
import com.mohiva.play.silhouette.core.providers.PasswordInfo
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO

trait MongoRepositories {

  lazy val userRepository: UserRepository = new MongoUserRepository()
  lazy val passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo] = new MongoPasswordRepository()

}