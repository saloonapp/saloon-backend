package common

import com.github.tototoshi.csv._

object Defaults {
  val contactName = "L'équipe SalooN"
  val contactEmail = "contact@saloonapp.co"
  val secureUrl = false // when generating url, generate the secure one (https)
  implicit object csvFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }
}