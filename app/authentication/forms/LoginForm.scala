package authentication.forms

import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.core.providers.Credentials

object LoginForm {
  val credentials = Form(
    mapping(
      "identifier" -> nonEmptyText,
      "password" -> nonEmptyText)(Credentials.apply)(Credentials.unapply))
  val email = Form(single("email" -> nonEmptyText))
}
