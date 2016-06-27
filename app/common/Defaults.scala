package common

import common.models.values.typed.Email
import common.models.values.typed.WebsiteUrl
import com.github.tototoshi.csv._
import org.joda.time.format.DateTimeFormat

object Defaults {
  val contactName = "L'équipe SalooN"
  val contactEmail = Email("contact@saloonapp.co")
  val adminEmail = Email("loicknuchel@gmail.com")
  val baseUrl = "http://saloonapp.herokuapp.com"
  val androidStoreUrl = WebsiteUrl("https://play.google.com/store/apps/details?id=co.saloonapp.eventexplorer")
  val appleStoreUrl = WebsiteUrl("https://itunes.apple.com/fr/app/saloon-events/id999897097")
  val dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")
  val secureUrl = false // when generating url, generate the secure one (https)
  implicit object csvFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }
}