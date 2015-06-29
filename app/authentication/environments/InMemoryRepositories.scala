package authentication.environments

import authentication.repositories.UserRepository
import authentication.repositories.impl.InMemoryUserRepository
import authentication.repositories.impl.InMemoryPasswordRepository
import com.mohiva.play.silhouette.core.providers.PasswordInfo
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO

trait InMemoryRepositories {

  lazy val userRepository: UserRepository = new InMemoryUserRepository()
  lazy val passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo] = new InMemoryPasswordRepository()

}