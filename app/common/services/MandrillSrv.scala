package common.services

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

object MandrillSrv {
  val baseUrl = "https://mandrillapp.com/api/1.0"
  val key = "md9UcB8wRMp480u9VfGIpw"

  def sendEmail(data: EmailData): Future[JsValue] = {
    WS.url(baseUrl + "/messages/send.json").post(Json.obj(
      "key" -> key,
      "message" -> Json.obj(
        "html" -> data.html,
        "text" -> data.text,
        "subject" -> data.subject,
        "from_email" -> data.fromEmail,
        "from_name" -> data.fromName,
        "to" -> Json.arr(
          Json.obj("email" -> data.to, "type" -> "to"))))).map { response =>
      response.json
    }
  }
}