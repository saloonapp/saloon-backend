package authentication.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.user.Request
import common.models.user.AccountRequest
import common.models.user.UserInvite
import common.repositories.user.UserRepository
import common.repositories.user.RequestRepository
import common.services.EmailSrv
import common.services.MandrillSrv
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

object Auth extends Silhouette[User, CachedCookieAuthenticator] with SilhouetteEnvironment {

  def login = UserAwareAction { implicit request =>
    request.identity match {
      case Some(user) => Redirect(backend.controllers.routes.Application.index)
      case None => Ok(authentication.views.html.login(LoginForm.credentials, LoginForm.email))
    }
  }

  def doLogin = Action.async { implicit request =>
    LoginForm.credentials.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.login(formWithErrors, LoginForm.email))),
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

  def doAccountRequest = Action.async { implicit request =>
    LoginForm.email.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.login(LoginForm.credentials, formWithErrors))),
      formData => {
        val email = formData
        val res: Future[Future[Result]] = for {
          userOpt <- UserRepository.getByEmail(email)
          reqOpt <- RequestRepository.getPendingAccountRequestByEmail(email)
        } yield {
          userOpt.map { u =>
            Future(Redirect(authentication.controllers.routes.Auth.login).flashing("error" -> s"L'email $email est déjà utilisé !"))
          }.getOrElse {
            reqOpt.map { req => Future(req.uuid) }.getOrElse {
              val accountRequest = Request.accountRequest(email)
              RequestRepository.insert(accountRequest).map { err => accountRequest.uuid }
            }.flatMap { requestId =>
              val emailData = EmailSrv.generateAccountRequestEmail(email, requestId)
              MandrillSrv.sendEmail(emailData).map { res =>
                Redirect(authentication.controllers.routes.Auth.login).flashing("success" -> s"Invitation envoyée à $email")
              }
            }
          }
        }
        res.flatMap(identity)
      })
  }

  def createAccount(requestId: String) = Action.async { implicit request =>
    RequestRepository.getPending(requestId).map { reqOpt =>
      reqOpt.map { req =>
        req.content match {
          case accountRequest: AccountRequest => {
            RequestRepository.update(req.copy(content = accountRequest.copy(visited = accountRequest.visited + 1)))
            Ok(authentication.views.html.createAccount(requestId, RegisterForm.form))
          }
          case userInvite: UserInvite => {
            RequestRepository.update(req.copy(content = userInvite.copy(visited = userInvite.visited + 1)))
            Ok(authentication.views.html.createAccount(requestId, RegisterForm.form))
          }
          case _ => BadRequest(backend.views.html.error("403", "Bad Request", false))
        }
      }.getOrElse(NotFound(backend.views.html.error("404", "Not found...", false)))
    }
  }

  def doCreateAccount(requestId: String) = Action.async { implicit request =>
    RegisterForm.form.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.createAccount(requestId, formWithErrors))),
      formData => {
        RequestRepository.getPending(requestId).flatMap { reqOpt =>
          reqOpt.map { req =>
            val emailOpt: Option[String] = req.content match {
              case accountRequest: AccountRequest => Some(accountRequest.email)
              case userInvite: UserInvite => Some(userInvite.email)
              case _ => None
            }
            emailOpt.map { email =>
              val loginInfo = LoginInfo(CredentialsProvider.Credentials, email)
              val authInfo = passwordHasher.hash(formData.password)
              val user = User(loginInfo = loginInfo, email = email, info = UserInfo(formData.firstName, formData.lastName))

              val result = for {
                userOpt <- UserRepository.insert(user)
                authInfo <- authInfoService.save(loginInfo, authInfo)
                authenticatorOpt <- env.authenticatorService.create(user)
                requestUpdated <- RequestRepository.setAccepted(requestId)
              } yield authenticatorOpt.map { authenticator =>
                env.eventBus.publish(SignUpEvent(user, request, request2lang))
                env.eventBus.publish(LoginEvent(user, request, request2lang))
                env.authenticatorService.send(authenticator, Redirect(backend.controllers.routes.Application.index))
              }.getOrElse {
                throw new AuthenticationException("Couldn't create an authenticator")
              }

              result.recover { case UserCreationException(msg, t) => Forbidden(msg) }
            }.getOrElse(Future(BadRequest(backend.views.html.error("403", "Bad Request"))))
          }.getOrElse(Future(NotFound(backend.views.html.error("404", "Not found..."))))
        }
      })
  }

  def logout = SecuredAction { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))
    env.authenticatorService.discard(Redirect("/"))
  }

}
