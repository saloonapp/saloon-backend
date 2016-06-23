package common.services

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

object SendGridSrv {
  val baseUrl = "https://api.sendgrid.com/v3"
  val key = play.api.Play.current.configuration.getString("sendgrid.key").getOrElse("Key Not Found !")

  // cf https://sendgrid.com/docs/API_Reference/Web_API_v3/Mail/index.html
  def sendEmail(data: EmailData): Future[Boolean] = {
    WS.url(baseUrl + "/mail/send").withHeaders(
      "Authorization" -> s"Bearer $key",
      "Content-Type" -> "application/json"
    ).post(Json.obj(
      "personalizations" -> Json.arr(
        Json.obj(
          "to" -> Json.arr(Json.obj("email" -> data.to)),
          "subject" -> data.subject
        )
      ),
      "from" -> Json.obj(
        "email" -> data.fromEmail,
        "name" -> data.fromName
      ),
      "content" -> Json.arr(
        Json.obj("type" -> "text/plain", "value" -> data.text),
        Json.obj("type" -> "text/html", "value" -> data.html)
      )
    )).map { response =>
      val success = 200 <= response.status && response.status < 300
      if(!success){ play.Logger.warn("Error sending email with SendGrid ("+response.status+"): "+response.body) }
      success
    }
  }
}
