package authentication.models

import common.models.values.typed.FirstName
import common.models.values.typed.LastName

case class RegisterInfo(
  firstName: FirstName,
  lastName: LastName,
  password: String)
