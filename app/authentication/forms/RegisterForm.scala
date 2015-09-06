package authentication.forms

import common.models.values.typed.FirstName
import common.models.values.typed.LastName
import authentication.models.RegisterInfo
import play.api.data.Form
import play.api.data.Forms._

object RegisterForm {
  val form = Form(
    mapping(
      "firstName" -> of[FirstName],
      "lastName" -> of[LastName],
      "password" -> nonEmptyText)(RegisterInfo.apply)(RegisterInfo.unapply))
}
