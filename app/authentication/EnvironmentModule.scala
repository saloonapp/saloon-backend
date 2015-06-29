package authentication

import authentication.models.User
import authentication.daos.PasswordInfoDAO
import authentication.services.UserService
import authentication.services.UserServiceImpl
import play.api.Play
import play.api.Play.current
import com.mohiva.play.silhouette.core.Environment
import com.mohiva.play.silhouette.core.EventBus
import com.mohiva.play.silhouette.core.utils.Clock
import com.mohiva.play.silhouette.core.utils.CacheLayer
import com.mohiva.play.silhouette.core.utils.IDGenerator
import com.mohiva.play.silhouette.core.utils.PasswordHasher
import com.mohiva.play.silhouette.core.services.AuthInfoService
import com.mohiva.play.silhouette.core.services.AuthenticatorService
import com.mohiva.play.silhouette.core.providers.PasswordInfo
import com.mohiva.play.silhouette.core.providers.CredentialsProvider
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.contrib.services.DelegableAuthInfoService
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticatorService
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticatorSettings
import com.mohiva.play.silhouette.contrib.utils.BCryptPasswordHasher
import com.mohiva.play.silhouette.contrib.utils.PlayCacheLayer
import com.mohiva.play.silhouette.contrib.utils.SecureRandomIDGenerator

trait EnvironmentModule {

  lazy val userService: UserService = new UserServiceImpl
  lazy val authenticatorService: AuthenticatorService[CachedCookieAuthenticator] = {
    new CachedCookieAuthenticatorService(CachedCookieAuthenticatorSettings(
      cookieName = Play.configuration.getString("silhouette.authenticator.cookieName").get,
      cookiePath = Play.configuration.getString("silhouette.authenticator.cookiePath").get,
      cookieDomain = Play.configuration.getString("silhouette.authenticator.cookieDomain"),
      secureCookie = Play.configuration.getBoolean("silhouette.authenticator.secureCookie").get,
      httpOnlyCookie = Play.configuration.getBoolean("silhouette.authenticator.httpOnlyCookie").get,
      cookieIdleTimeout = Play.configuration.getInt("silhouette.authenticator.cookieIdleTimeout").get,
      cookieAbsoluteTimeout = Play.configuration.getInt("silhouette.authenticator.cookieAbsoluteTimeout"),
      authenticatorExpiry = Play.configuration.getInt("silhouette.authenticator.authenticatorExpiry").get), cacheLayer, idGenerator, Clock())
  }
  lazy val cacheLayer: CacheLayer = new PlayCacheLayer
  lazy val idGenerator: IDGenerator = new SecureRandomIDGenerator
  lazy val passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo] = new PasswordInfoDAO
  lazy val passwordHasher: PasswordHasher = new BCryptPasswordHasher
  lazy val authInfoService: AuthInfoService = new DelegableAuthInfoService(passwordInfoDAO)
  lazy val credentialsProvider: CredentialsProvider = new CredentialsProvider(authInfoService, passwordHasher, Seq(passwordHasher))
  lazy val eventBus = EventBus()

  implicit lazy val env: Environment[User, CachedCookieAuthenticator] = {
    Environment[User, CachedCookieAuthenticator](
      userService,
      authenticatorService,
      Map(credentialsProvider.id -> credentialsProvider),
      eventBus)
  }

}
