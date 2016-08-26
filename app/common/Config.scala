package common

import com.github.tototoshi.csv.DefaultCSVFormat
import common.models.values.typed.{WebsiteUrl, Email}
import org.joda.time.format.DateTimeFormat
import play.api.Play.current

object Config {
  private val configuration = current.configuration
  object Application {
    val env: String = configuration.getString("application.env").get // possible values : 'local', 'dev', 'prod'
    val isProd: Boolean = env == "prod"
    val baseUrl: String = "http://www.saloonapp.co"
    val googlePlayUrl: WebsiteUrl = WebsiteUrl("https://play.google.com/store/apps/details?id=co.saloonapp.mobile")
    val iTunesUrl: WebsiteUrl = WebsiteUrl("https://itunes.apple.com/fr/app/saloon-events/id999897097")
    val dateFormat = "dd/MM/yyyy"
    val datetimeFormat = "dd/MM/yyyy HH:mm"
    val dateFormatter = DateTimeFormat.forPattern(dateFormat)
    val datetimeFormatter = DateTimeFormat.forPattern(datetimeFormat)
    val secureUrl = false // when generating url, generate the secure one (https)
    implicit object csvFormat extends DefaultCSVFormat {
      override val delimiter = ';'
    }
  }
  object Contact {
    val name: String = "L'équipe SalooN"
    val email: Email = Email("contact@saloonapp.co")
  }
  object Admin {
    val email: Email = Email("loicknuchel@gmail.com")
  }
  object SendGrid {
    val key: String = configuration.getString("sendgrid.key").get
    val uri: String = "https://api.sendgrid.com/v3"
  }
  object MailChimp {
    val key: String = configuration.getString("mailchimp.key").get
    val uri: String = "https://<dc>.api.mailchimp.com/3.0".replace("<dc>", key.split("-")(1))
  }
  object Google {
    object Maps {
      val key: String = configuration.getString("google.maps.key").get
    }
  }
  object Twitter {
    val account: String = configuration.getString("twitter.account").get
  }
}
