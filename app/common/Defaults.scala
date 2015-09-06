package common

import common.models.values.typed.Email
import com.github.tototoshi.csv._

object Defaults {
  val contactName = "L'équipe SalooN"
  val contactEmail = Email("contact@saloonapp.co")
  val secureUrl = false // when generating url, generate the secure one (https)
  implicit object csvFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }
}