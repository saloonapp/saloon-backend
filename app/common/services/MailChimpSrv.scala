package common.services

import common.Config
import common.models.values.typed.{TextHTML, Email}
import conferences.models.Conference
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

object MailChimp {
  object Lists {
    val ConferenceListNewsletter = "cd0abf22f4"
  }
  object Campaigns {
    object Folders {
      val ConferenceListNewsletter = "96d0bc75ff"
      val ConferenceListNewsletterTEST = "7f75b156c8"
    }
  }
}

case class MailChimpCampaign(
  category: String,
  recipientListId: String,
  folderId: String,
  name: String,
  senderName: String,
  senderMail: Email,
  subject: String,
  contentHtml: TextHTML,
  social: JsValue)
object MailChimpCampaign {
  def conferenceListNewsletter(closingCFPs: List[Conference], incomingConferences: List[Conference], newData: List[(Conference, Map[String, Boolean])]): MailChimpCampaign = MailChimpCampaign(
    category = "regular",
    recipientListId = MailChimp.Lists.ConferenceListNewsletter,
    folderId = MailChimp.Campaigns.Folders.ConferenceListNewsletter,
    name = "Conference List Newsletter du "+(new DateTime().toString("dd/MM/yyyy HH:mm")), // internal use only
    senderName = "Conference List by SalooN",
    senderMail = Config.Contact.email,
    subject = "Nouveautés des conférences tech en France",
    contentHtml = TextHTML(conferences.views.html.emails.newsletter(Config.Application.baseUrl, closingCFPs, incomingConferences, newData).toString),
    social = Json.obj(
      "image_url" -> "https://pbs.twimg.com/profile_images/746325473895014400/hotwvNKV_400x400.jpg",
      "title" -> ("Conference List Newsletter du "+(new DateTime().toString("dd/MM/yyyy HH:mm"))),
      "description" -> List(
        if(closingCFPs.length > 0) Some(closingCFPs.length+" CFPs bientôt terminés") else None,
        if(incomingConferences.length > 0) Some(incomingConferences.length+" conférences approchant") else None,
        if(newData.filter(_._2.getOrElse("videos", false)).length > 0) Some(newData.filter(_._2.getOrElse("videos", false)).length+" vidéos de conférences publiées") else None
      ).flatten.mkString(", ")
    ))

  def conferenceListNewsletterTest(closingCFPs: List[Conference], incomingConferences: List[Conference], newData: List[(Conference, Map[String, Boolean])]): MailChimpCampaign =
    conferenceListNewsletter(closingCFPs, incomingConferences, newData).copy(folderId = MailChimp.Campaigns.Folders.ConferenceListNewsletterTEST)
}

object MailChimpSrv {
  val key = Config.MailChimp.key
  val baseUrl = Config.MailChimp.uri

  def createAndSendCampaign(campaign: MailChimpCampaign): Future[String] = {
    for {
      (id, url) <- createCampaign(campaign)
      res1 <- setCampaignContent(id, campaign)
      res2 <- sendCampaign(id)
    } yield url
  }
  def createAndTestCampaign(campaign: MailChimpCampaign, emails: List[String]): Future[String] = {
    for {
      (id, url) <- createCampaign(campaign)
      res1 <- setCampaignContent(id, campaign)
      res2 <- testCampaign(id, emails)
    } yield url
  }

  // cf http://developer.mailchimp.com/documentation/mailchimp/reference/campaigns/
  private def createCampaign(campaign: MailChimpCampaign): Future[(String, String)] = {
    WS.url(baseUrl + "/campaigns").withHeaders(
      "Authorization" -> s"apikey: $key",
      "Content-Type" -> "application/json"
    ).post(Json.obj(
      "type" -> campaign.category,
      "recipients" -> Json.obj(
        "list_id" -> campaign.recipientListId
      ),
      "settings" -> Json.obj(
        "folder_id" -> campaign.folderId,
        "title" -> campaign.name,
        "from_name" -> campaign.senderName,
        "reply_to" -> campaign.senderMail,
        "subject_line" -> campaign.subject,
        "inline_css" -> false,
        "auto_footer" -> false,
        "auto_tweet" -> false,
        "fb_comments" -> false,
        "use_conversation" -> false
      ),
      "tracking" -> Json.obj(
        "opens" -> true,
        "html_clicks" -> true,
        "text_clicks" -> true,
        "goal_tracking" -> false,
        "ecomm360" -> false
      ),
      "social_card" -> campaign.social
    )).map(response => {
      //play.Logger.info("createCampaign ("+response.status+"): "+response.body)
      ((response.json \ "id").as[String], (response.json \ "archive_url").as[String])
    })
  }
  private def setCampaignContent(campaignId: String, campaign: MailChimpCampaign): Future[Boolean] = {
    WS.url(baseUrl + s"/campaigns/$campaignId/content").withHeaders(
      "Authorization" -> s"apikey: $key",
      "Content-Type" -> "application/json"
    ).put(Json.obj(
      "html" -> campaign.contentHtml
    )).map(response => {
      //play.Logger.info("setCampaignContent ("+response.status+"): "+response.body)
      response.status == 200
    })
  }
  private def testCampaign(campaignId: String, emails: List[String]): Future[Boolean] = {
    WS.url(baseUrl + s"/campaigns/$campaignId/actions/test").withHeaders(
      "Authorization" -> s"apikey: $key",
      "Content-Type" -> "application/json"
    ).post(Json.obj(
      "test_emails" -> emails,
      "send_type" -> "html"
    )).map(response => {
      //play.Logger.info("testCampaign ("+response.status+"): "+response.body)
      response.status == 204
    })
  }
  private def sendCampaign(campaignId: String): Future[Boolean] = {
    if(!Config.Application.isProd){ throw new IllegalStateException("You should be in Prod environment to send a MailChimp Campaign !") }
    WS.url(baseUrl + s"/campaigns/$campaignId/actions/send").withHeaders(
      "Authorization" -> s"apikey: $key",
      "Content-Type" -> "application/json"
    ).post(Json.obj()).map(response => {
      //play.Logger.info("sendCampaign ("+response.status+"): "+response.body)
      response.status == 204
    })
  }
}
