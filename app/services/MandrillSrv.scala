package services

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

object MandrillSrv {
  val baseUrl = "https://mandrillapp.com/api/1.0"
  val key = "md9UcB8wRMp480u9VfGIpw"
  val senderEmail = "bob@saloonapp.co"
  val senderName = "Bob de SalooN"

  def sendEmail(data: EmailData): Future[JsValue] = {
    WS.url(baseUrl + "/messages/send.json").post(Json.obj(
      "key" -> key,
      "message" -> Json.obj(
        "html" -> data.html,
        "text" -> data.text,
        "subject" -> data.subject,
        "from_email" -> senderEmail,
        "from_name" -> senderName,
        "to" -> Json.arr(
          Json.obj("email" -> data.to, "type" -> "to"))))).map { response =>
      response.json
    }
  }
}