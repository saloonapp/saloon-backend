package authentication.controllers

import authentication.forms.LoginForm
import authentication.forms.RegisterForm
import authentication.models.User
import authentication.environments.SilhouetteEnvironment
import authentication.repositories.UserCreationException
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import com.mohiva.play.silhouette.core.LoginEvent
import com.mohiva.play.silhouette.core.LogoutEvent
import com.mohiva.play.silhouette.core.SignUpEvent
import com.mohiva.play.silhouette.core.LoginInfo
import com.mohiva.play.silhouette.core.Silhouette
import com.mohiva.play.silhouette.core.exceptions.AuthenticationException
import com.mohiva.play.silhouette.core.providers.Credentials
import com.mohiva.play.silhouette.core.providers.CredentialsProvider
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator

/**
 * @param env The Silhouette environment.
 * @param userService The user service implementation.
 * @param authInfoService The auth info service implementation.
 * @param avatarService The avatar service implementation.
 * @param passwordHasher The password hasher implementation.
 */
object Auth extends Silhouette[User, CachedCookieAuthenticator] with SilhouetteEnvironment {

  /*def index = UserAwareAction { implicit request =>
    val userName = request.identity match {
      case Some(identity) => identity.username
      case None => "Guest"
    }
    Ok(authentication.views.html.index(request.identity, s"Hello $userName"))
  }*/

  def login = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(backend.controllers.routes.Application.index)
      case None => Ok(authentication.views.html.login(LoginForm.form))
    }
  }

  def register = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(backend.controllers.routes.Application.index)
      case None => Ok(authentication.views.html.register(RegisterForm.form))
    }
  }

  def logout = SecuredAction { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))
    env.authenticatorService.discard(Redirect("/"))
  }

  def doLogin = Action.async { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(authentication.views.html.login(formWithErrors))),
      formData => {
        val r = for {
          loginInfo <- env.providers.get(CredentialsProvider.Credentials) match {
            case Some(p: CredentialsProvider) => p.authenticate(formData)
            case _ => Future.failed(new AuthenticationException(s"Cannot find credentials provider"))
          }
          resp <- userRepository.retrieve(loginInfo).flatMap {
            case Some(user) => env.authenticatorService.create(user).map {
              case Some(authenticator) =>
                env.eventBus.publish(LoginEvent(user, request, request2lang))
                env.authenticatorService.send(authenticator, Redirect(backend.controllers.routes.Application.index))
              case None => throw new AuthenticationException("Couldn't create an authenticator")
            }
            case None => Future.failed(new AuthenticationException("Couldn't find user"))
          }
        } yield resp
        r.recoverWith(exceptionHandler)
      })
  }

  def doRegister = Action.async { implicit request =>
    RegisterForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(authentication.views.html.register(form))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.Credentials, data.username)
        val authInfo = passwordHasher.hash(data.password)
        val user = User(
          loginInfo = loginInfo,
          username = data.username,
          email = data.email)
        val result = for {
          user <- userRepository.save(user)
          authInfo <- authInfoService.save(loginInfo, authInfo)
          maybeAuthenticator <- env.authenticatorService.create(user)
        } yield {
          maybeAuthenticator match {
            case Some(authenticator) =>
              env.eventBus.publish(SignUpEvent(user, request, request2lang))
              env.eventBus.publish(LoginEvent(user, request, request2lang))
              env.authenticatorService.send(authenticator, Redirect(backend.controllers.routes.Application.index))
            case None => throw new AuthenticationException("Couldn't create an authenticator")
          }
        }
        result.recover {
          case UserCreationException(msg, t) => Forbidden(msg)
        }
      })
  }

}
