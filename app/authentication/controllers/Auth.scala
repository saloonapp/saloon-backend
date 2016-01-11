package authentication.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.user.Request
import common.models.user.RequestId
import common.models.user.AccountRequest
import common.models.user.AccountInvite
import common.models.user.PasswordReset
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

  def login = UserAwareAction { implicit req =>
    req.identity match {
      case Some(user) => Redirect(backend.controllers.routes.Application.index)
      case None => Ok(authentication.views.html.login(LoginForm.credentials, LoginForm.email))
    }
  }

  def doLogin = Action.async { implicit req =>
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
                env.eventBus.publish(LoginEvent(user, req, request2lang))
                env.authenticatorService.send(authenticator, Redirect(backend.controllers.routes.Application.index))
              case None => throw new AuthenticationException("Couldn't create an authenticator")
            }
            case None => Future.failed(new AuthenticationException("Couldn't find user"))
          }
        } yield resp
        r.recoverWith(exceptionHandler)
      })
  }

  def doAccountRequest = Action.async { implicit req =>
    LoginForm.email.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.login(LoginForm.credentials, formWithErrors))),
      formData => {
        val email = formData
        val res: Future[Future[Result]] = for {
          userOpt <- UserRepository.getByEmail(email)
          requestOpt <- RequestRepository.getPendingAccountRequestByEmail(email)
        } yield {
          userOpt.map { u =>
            Future(Redirect(authentication.controllers.routes.Auth.login).flashing("error" -> s"L'email ${email.unwrap} est déjà utilisé !"))
          }.getOrElse {
            requestOpt.map { request => Future(request.uuid) }.getOrElse {
              val accountRequest = Request.accountRequest(email)
              RequestRepository.insert(accountRequest).map { err => accountRequest.uuid }
            }.flatMap { requestId =>
              val emailData = EmailSrv.generateAccountRequestEmail(email, requestId)
              MandrillSrv.sendEmail(emailData).map { res =>
                Redirect(authentication.controllers.routes.Auth.login).flashing("success" -> s"Invitation envoyée à ${email.unwrap}")
              }
            }
          }
        }
        res.flatMap(identity)
      })
  }

  def createAccount(requestId: RequestId) = Action.async { implicit req =>
    RequestRepository.getPending(requestId).map { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case accountRequest: AccountRequest => {
            RequestRepository.update(request.copy(content = accountRequest.copy(visited = accountRequest.visited + 1))) // TODO : replace with : RequestRepository.incrementVisited(requestId)
            Ok(authentication.views.html.createAccount(requestId, RegisterForm.form))
          }
          case accountInvite: AccountInvite => {
            RequestRepository.update(request.copy(content = accountInvite.copy(visited = accountInvite.visited + 1))) // TODO : replace with : RequestRepository.incrementVisited(requestId)
            Ok(authentication.views.html.createAccount(requestId, RegisterForm.form))
          }
          case _ => BadRequest(backend.views.html.error("403", "Bad Request", false))
        }
      }.getOrElse(NotFound(backend.views.html.error("404", "Not found...", false)))
    }
  }

  def doCreateAccount(requestId: RequestId) = Action.async { implicit req =>
    RegisterForm.form.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.createAccount(requestId, formWithErrors))),
      formData => {
        RequestRepository.getPending(requestId).flatMap { requestOpt =>
          requestOpt.map { request =>
            val (emailOpt, redirect) = request.content match {
              case AccountRequest(email, _, _) => (Some(email), Redirect(backend.controllers.routes.Application.welcome))
              case AccountInvite(email, nextOpt, _, _) => (Some(email), nextOpt.map(next => Redirect(backend.controllers.routes.Requests.details(next))).getOrElse(Redirect(backend.controllers.routes.Application.welcome)))
              case _ => (None, Redirect(backend.controllers.routes.Application.welcome))
            }
            emailOpt.map { email =>
              val loginInfo = LoginInfo(CredentialsProvider.Credentials, email.unwrap)
              val authInfo = passwordHasher.hash(formData.password)
              val user = User(loginInfo = loginInfo, email = email, info = UserInfo(formData.firstName, formData.lastName))

              val result = for {
                userOpt <- UserRepository.insert(user)
                authInfo <- authInfoService.save(loginInfo, authInfo)
                authenticatorOpt <- env.authenticatorService.create(user)
                requestUpdated <- RequestRepository.setAccepted(requestId)
              } yield authenticatorOpt.map { authenticator =>
                env.eventBus.publish(SignUpEvent(user, req, request2lang))
                env.eventBus.publish(LoginEvent(user, req, request2lang))
                env.authenticatorService.send(authenticator, redirect)
              }.getOrElse {
                throw new AuthenticationException("Couldn't create an authenticator")
              }

              result.recover { case UserCreationException(msg, t) => Forbidden(msg) }
            }.getOrElse(Future(BadRequest(backend.views.html.error("403", "Bad Request"))))
          }.getOrElse(Future(NotFound(backend.views.html.error("404", "Not found..."))))
        }
      })
  }

  def doPasswordResetRequest = Action.async { implicit req =>
    LoginForm.email.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.login(LoginForm.credentials, formWithErrors))),
      formData => {
        val email = formData
        UserRepository.getByEmail(email).flatMap(userOpt => {
          userOpt.map { u =>
            val passwordReset = Request.passwordReset(email)
            RequestRepository.insert(passwordReset).flatMap { err =>
              val emailData = EmailSrv.generatePasswordResetRequestEmail(email, passwordReset.uuid)
              MandrillSrv.sendEmail(emailData).map { res =>
                Redirect(authentication.controllers.routes.Auth.login).flashing("success" -> s"Demande de réinitialisation du mot de passe envoyée")
              }
            }
          }.getOrElse {
            Future(Redirect(authentication.controllers.routes.Auth.login).flashing("error" -> s"L'email ${email.unwrap} n'existe pas !"))
          }
        })
      })
  }

  def passwordReset(requestId: RequestId) = Action.async { implicit req =>
    RequestRepository.getPasswordReset(requestId).map { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case passwordReset: PasswordReset => {
            RequestRepository.update(request.copy(content = passwordReset.copy(visited = passwordReset.visited + 1))) // TODO : replace with : RequestRepository.incrementVisited(requestId)
            Ok(authentication.views.html.passwordReset(requestId, LoginForm.passwordReset))
          }
          case _ => BadRequest(backend.views.html.error("403", "Bad Request", false))
        }
      }.getOrElse(NotFound(backend.views.html.error("404", "Not found...", false)))
    }
  }

  def doPasswordReset(requestId: RequestId) = Action.async { implicit req =>
    LoginForm.passwordReset.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(authentication.views.html.passwordReset(requestId, formWithErrors))),
      formData => {
        val password = formData
        RequestRepository.getPasswordReset(requestId).flatMap { requestOpt =>
          requestOpt.map { request =>
            request.content match {
              case PasswordReset(email, _, _) => {
                UserRepository.getByEmail(email).flatMap(_.map(user => {
                  val authInfo = passwordHasher.hash(password)
                  for {
                    authInfo <- authInfoService.save(user.loginInfo, authInfo)
                    authenticatorOpt <- env.authenticatorService.create(user)
                    requestUpdated <- RequestRepository.setAccepted(requestId)
                  } yield authenticatorOpt.map { authenticator =>
                    env.eventBus.publish(LoginEvent(user, req, request2lang))
                    env.authenticatorService.send(authenticator, Redirect(backend.controllers.routes.Application.index))
                  }.getOrElse {
                    throw new AuthenticationException("Couldn't create an authenticator")
                  }
                }).getOrElse {
                  Future(BadRequest(backend.views.html.error("403", "Bad Request")))
                })
              }
              case _ => Future(Redirect(backend.controllers.routes.Application.welcome))
            }
          }.getOrElse(Future(NotFound(backend.views.html.error("404", "Not found..."))))
        }
      })
  }

  def logout = SecuredAction { implicit req =>
    env.eventBus.publish(LogoutEvent(req.identity, req, request2lang))
    env.authenticatorService.discard(Redirect("/"))
  }

}
