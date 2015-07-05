package authentication.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.repositories.user.UserRepository
import authentication.models.RegisterInfo
import authentication.forms.LoginForm
import authentication.forms.RegisterForm
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

  def register = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(backend.controllers.routes.Application.index)
      case None => Ok(authentication.views.html.register(RegisterForm.form))
    }
  }

  def doRegister = Action.async { implicit request =>
    RegisterForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(authentication.views.html.register(formWithErrors))),
      formData => {
        val loginInfo = LoginInfo(CredentialsProvider.Credentials, formData.email)
        val authInfo = passwordHasher.hash(formData.password)
        val user = User(
          loginInfo = loginInfo,
          email = formData.email,
          info = UserInfo(
            formData.firstName,
            formData.lastName))
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

  def logout = SecuredAction { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))
    env.authenticatorService.discard(Redirect("/"))
  }

  def createAccount(userId: String) = UserAwareAction.async { implicit request =>
    UserRepository.getByUuid(userId).map { userOpt =>
      var registerForm = userOpt.map { user =>
        RegisterForm.form.fill(RegisterInfo(user.info.firstName, user.info.lastName, user.email, ""))
      }.getOrElse(RegisterForm.form)
      Ok(authentication.views.html.createAccount(userOpt, registerForm))
    }
  }

  def doCreateAccount(userId: String) = Action.async { implicit request =>
    UserRepository.getByUuid(userId).flatMap { userOpt =>
      RegisterForm.form.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(authentication.views.html.createAccount(userOpt, formWithErrors))),
        formData => {
          userOpt.map { user =>
            val loginInfo = LoginInfo(CredentialsProvider.Credentials, formData.email)
            val authInfo = passwordHasher.hash(formData.password)
            val newUser = user.copy(
              loginInfo = loginInfo,
              email = formData.email,
              info = UserInfo(
                formData.firstName,
                formData.lastName))
            val result = for {
              user <- UserRepository.update(userId, newUser)
              authInfo <- authInfoService.save(loginInfo, authInfo)
              maybeAuthenticator <- env.authenticatorService.create(newUser)
            } yield {
              maybeAuthenticator match {
                case Some(authenticator) =>
                  env.eventBus.publish(SignUpEvent(newUser, request, request2lang))
                  env.eventBus.publish(LoginEvent(newUser, request, request2lang))
                  env.authenticatorService.send(authenticator, Redirect(backend.controllers.routes.Application.index))
                case None => throw new AuthenticationException("Couldn't create an authenticator")
              }
            }
            result.recover {
              case UserCreationException(msg, t) => Forbidden(msg)
            }
          }.getOrElse {
            Future(Ok(authentication.views.html.createAccount(userOpt, RegisterForm.form)))
          }
        })
    }
  }
}
