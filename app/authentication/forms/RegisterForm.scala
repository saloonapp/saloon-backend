package authentication.forms

import authentication.models.RegisterInfo
import play.api.data.Form
import play.api.data.Forms._

object RegisterForm {
  val form = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText)(RegisterInfo.apply)(RegisterInfo.unapply))
}
