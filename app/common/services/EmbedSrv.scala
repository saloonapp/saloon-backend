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
  // TODO : slides.com, speakerdeck...
  val youtubeUrl = "https?://www.youtube.com/watch\\?(?:[^=]+=[^&]+&)*v=([^&]+).*".r
  val vimeoUrl = "https?://vimeo.com/([^/?#]+).*".r
  val slideshareUrl = "https?://[a-z]+.slideshare.net/([^/?#]+)/([^/?#]+).*".r
  val googleSlidesUrl = "https?://docs.google.com/presentation/d/([^/?#]+).*".r

  def embedCode(url: String): Future[Option[EmbedData]] = url match {
    case youtubeUrl(videoId) => youtubeEmbedCode(url, videoId)
    case vimeoUrl(videoId) => vimeoEmbedCode(url, videoId)
    case slideshareUrl(user, slidesId) => slideshareEmbedCode(url)
    case googleSlidesUrl(slidesId) => googleSlidesEmbedCode(url, slidesId)
    case _ => Future(None)
  }
  private def youtubeEmbedCode(url: String, videoId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://www.youtube.com/embed/$videoId"
    Future(Some(EmbedData(
      url,
      "youtube",
      embedUrl,
      s"""<iframe width="560" height="315" src="$embedUrl" frameborder="0" allowfullscreen></iframe>""")))
  }
  private def vimeoEmbedCode(url: String, videoId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://player.vimeo.com/video/$videoId"
    Future(Some(EmbedData(
      url,
      "vimeo",
      embedUrl,
      s"""<iframe src="$embedUrl" width="640" height="360" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>""")))
  }
  private def googleSlidesEmbedCode(url: String, slidesId: String): Future[Option[EmbedData]] = {
    val embedUrl = s"https://docs.google.com/presentation/d/$slidesId/embed"
    Future(Some(EmbedData(
      url,
      "googleSlides",
      embedUrl,
      s"""<iframe src="$embedUrl" width="960" height="569" frameborder="0" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true"></iframe>""")))
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
          s"""<iframe src="$embedUrl" width="595" height="485" frameborder="0" marginwidth="0" marginheight="0" scrolling="no" style="border:1px solid #CCC; border-width:1px; margin-bottom:5px; max-width: 100%;" allowfullscreen></iframe>"""
        ))
      } else {
        None
      }
    }
  }
}
