package common.services

import org.jsoup.Jsoup
import play.api.libs.ws.WS

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import play.api.Play.current

case class EmbedData(
  originUrl: String,
  service: String,
  embedUrl: String,
  embedCode: String)
object EmbedData {
  implicit val format = Json.format[EmbedData]
  def unknown(url: String): EmbedData = EmbedData(url, "unknown", url, s"""<a href="$url" target="_blank">$url</a>""")
}
object EmbedSrv {
  // TODO : add prezi
  def embedCode(url: String): Future[Option[EmbedData]] = url match {
    case UrlSrv.youtube1(videoId) => youtubeEmbedCode(url, videoId)
    case UrlSrv.youtube2(videoId) => youtubeEmbedCode(url, videoId)
    case UrlSrv.dailymotion(videoId) => dailymotionEmbedCode(url, videoId)
    case UrlSrv.vimeo(videoId) => vimeoEmbedCode(url, videoId)
    case UrlSrv.slideshare(user, slidesId) => slideshareEmbedCode(url)
    case UrlSrv.speakerdeck(user, slidesId) => speakerdeckEmbedCode(url)
    case UrlSrv.slidescom(user, slidesId) => slidescomEmbedCode(url, user, slidesId)
    case UrlSrv.googleslides(slidesId) => googleslidesEmbedCode(url, slidesId)
    case UrlSrv.googledocs(slidesId) => googledocsEmbedCode(url, slidesId)
    case _ => unknownEmbedCode(url)
  }

  private def youtubeEmbedCode(url: String, videoId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://www.youtube.com/embed/$videoId"
    Future(Some(EmbedData(
      url,
      "youtube",
      embedUrl,
      s"""<iframe width="560" height="315" src="$embedUrl" frameborder="0" allowfullscreen></iframe>""")))
  }
  private def dailymotionEmbedCode(url: String, videoId: String): Future[Option[EmbedData]] = {
    val videoHash = videoId.split("_").headOption.getOrElse(videoId)
    val embedUrl = s"//www.dailymotion.com/embed/video/$videoHash"
    Future(Some(EmbedData(
      url,
      "dailymotion",
      embedUrl,
      s"""<iframe src="$embedUrl" width="560" height="315" frameborder="0" allowfullscreen></iframe>""")))
  }
  private def vimeoEmbedCode(url: String, videoId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://player.vimeo.com/video/$videoId"
    Future(Some(EmbedData(
      url,
      "vimeo",
      embedUrl,
      s"""<iframe src="$embedUrl" width="640" height="360" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>""")))
  }
  private def googleslidesEmbedCode(url: String, slidesId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://docs.google.com/presentation/d/$slidesId/embed"
    Future(Some(EmbedData(
      url,
      "googleslides",
      embedUrl,
      s"""<iframe src="$embedUrl" width="960" height="569" frameborder="0" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true"></iframe>""")))
  }
  private def googledocsEmbedCode(url: String, slidesId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://drive.google.com/file/d/$slidesId/preview"
    Future(Some(EmbedData(
      url,
      "googledocs",
      embedUrl,
      s"""<iframe src="$embedUrl" width="640" height="480"></iframe>""")))
  }
  private def slidescomEmbedCode(url: String, user: String, slidesId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"//slides.com/$user/$slidesId/embed?style=light"
    Future(Some(EmbedData(
      url,
      "slidescom",
      embedUrl,
      s"""<iframe src="$embedUrl" width="576" height="420" scrolling="no" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>""")))
  }
  private def slideshareEmbedCode(url: String): Future[Option[EmbedData]] = {
    WS.url(url).get().map { response =>
      val doc = Jsoup.parse(response.body)
      val embedUrl = doc.select("meta.twitter_player").attr("value")
      if(embedUrl != null && embedUrl.length > 0) {
        Some(EmbedData(
          url,
          "slideshare",
          embedUrl,
          s"""<iframe src="$embedUrl" width="595" height="485" frameborder="0" marginwidth="0" marginheight="0" scrolling="no" style="border:1px solid #CCC; border-width:1px; margin-bottom:5px; max-width: 100%;" allowfullscreen></iframe>"""))
      } else {
        None
      }
    }
  }
  private def speakerdeckEmbedCode(url: String): Future[Option[EmbedData]] = {
    WS.url(url).get().map { response =>
      val doc = Jsoup.parse(response.body)
      val embedElt = doc.select("div.speakerdeck-embed")
      val embedId = embedElt.attr("data-id")
      val embedRatio = embedElt.attr("data-ratio")
      if(embedId != null && embedId.length > 0) {
        Some(EmbedData(
          url,
          "speakerdeck",
          url,
          s"""<script async class="speakerdeck-embed" data-id="$embedId" data-ratio="$embedRatio" src="//speakerdeck.com/assets/embed.js"></script>"""))
      } else {
        None
      }
    }
  }
  private def unknownEmbedCode(url: String): Future[Option[EmbedData]] = {
    try {
      WS.url(url).get().map { response =>
        val doc = Jsoup.parse(response.body)
        if(doc.select(".reveal").size() > 0) {
          Some(EmbedData(
            url,
            "reveal",
            url,
            s"""<iframe src="$url" width="595" height="485" frameborder="0"></iframe>"""))
        } else if(doc.select("iframe").size() == 1 && doc.select("iframe").first().attr("src").startsWith("http")) {
          val iframe  = doc.select("iframe").first()
          Some(EmbedData(
            url,
            "iframe",
            iframe.attr("src"),
            iframe.outerHtml()))
        } else {
          None
        }
      }.recover {
        case e: Throwable => None
      }
    } catch {
      case e: Throwable => Future(None)
    }
  }
}
