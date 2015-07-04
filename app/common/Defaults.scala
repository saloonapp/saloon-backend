package common

import com.github.tototoshi.csv._

object Defaults {
  val contactEmail = "contact@saloonapp.co"
  implicit object csvFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }
}