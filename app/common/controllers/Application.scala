package common.controllers

import common.Utils
import common.models.values.typed.Email
import common.services.EmailSrv
import common.services.MandrillSrv
import common.repositories.user.UserRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import common.models.user.User
import authentication.environments.SilhouetteEnvironment
import com.mohiva.play.silhouette.core.Silhouette
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator

object Application extends Silhouette[User, CachedCookieAuthenticator] with SilhouetteEnvironment {
  val contactForm = Form(
    tuple(
      "url" -> nonEmptyText,
      "name" -> nonEmptyText,
      "email" -> of[Email],
      "message" -> nonEmptyText))

  def sendContactEmail = UserAwareAction.async { implicit request =>
    contactForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(Utils.getFormParam("url").get).flashing("error" -> "Tous les champs du formulaire sont obligatoires !")),
      formData => {
        formData match {
          case (url, name, email, message) =>
            val emailData = EmailSrv.generateContactEmail("http://" + request.host + url, name, email, message, request.identity)
            MandrillSrv.sendEmail(emailData).map { res =>
              Redirect(url).flashing("success" -> "Email de contact envoyé :)")
            }
        }
      })
  }

  def corsPreflight(all: String) = Action {
    Ok("").withHeaders(
      "Allow" -> "*",
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, userId, timestamp");
  }
}
