package common.services

import common.EnumUtils
import common.services.UrlInfo.{Service, Category}
import play.api.libs.json.{Reads, Json}
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}

object UrlSrv {
  /* URLS TO ADD
    - slides
    - video
    - apps
      - https://itunes.apple.com/us/app/one-epic-knight/id469820028?mt=8
      - https://play.google.com/store/apps/details?id=com.ironhide.games.clashoftheolympians
    - newspaper
      - https://www.theguardian.com/technology/2016/aug/06/nsa-zero-days-stockpile-security-vulnerability-defcon
    - blog
      - http://cookingwithamandaz.blogspot.fr/2016/08/13-reasons-to-never-purchase-snacks.html?utm_source=twitterfeed&utm_medium=twitter
    - social
      - https://www.instagram.com/p/BJamT9MBSHg/
      - http://vine.co/v/ebKbA9ubEZa
      - https://www.facebook.com/nytimes/videos/10150847440454999/?hc_location=ufi&utm_content=bufferbd74f&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer
    - ecommerce
      - http://deals.ebay.com/5003492080_Vince_Camuto_Shelly_1_Women__Open_Toe_Synthetic__Slides_Sandal?rmvSB=true
    - other
      - https://www.dropbox.com/s/3b08ae2m9owat66/Resolution%20Revolution%20Slideshow.pdf?dl=0
   */

  /* WRONG RESOLVE
    - http://tech.mg/E3zIme
   */

  val youtube1Url = "https?://www.youtube.com/watch\\?(?:[^=]+=[^&]+&)*v=([^&]+).*".r
  val youtube2Url = "https?://youtu.be/([^/?#]+).*".r
  val dailymotionUrl = "https?://www.dailymotion.com/video/([^/?#]+).*".r
  val vimeoUrl = "https?://vimeo.com/([^/?#]+).*".r
  val slideshareUrl = "https?://[a-z]+.slideshare.net/([^/?#]+)/([^/?#]+).*".r
  val speakerdeckUrl = "https?://speakerdeck.com/([^/?#]+)/([^/?#]+).*".r
  val slidescomUrl = "https?://slides.com/([^/?#]+)/([^/?#]+).*".r
  val preziUrl = "https?://prezi.com/p/([^/?#]+).*".r
  val googleslidesUrl = "https?://docs.google.com/presentation/d/([^/?#]+).*".r
  val googledocsUrl = "https?://drive.google.com/file/d/([^/?#]+).*".r
  val twitterUrl = "https?://twitter.com/([^/?#]+)/status/([^/?#]+).*".r
  val tcoUrl = "https?://t.co/([^/?#]+).*".r
  val owlyUrl = "https?://ow.ly/([^/?#]+).*".r
  val shstUrl = "https?://sh.st/([^/?#]+).*".r
  val mfttUrl = "https?://mf.tt/([^/?#]+).*".r
  val wpmeUrl = "https?://wp.me/([^/?#]+).*".r
  val fbmeUrl = "https?://fb.me/([^/?#]+).*".r
  val bitlyUrl = "https?://bit.ly/([^/?#]+).*".r
  val iftttUrl = "https?://ift.tt/([^/?#]+).*".r
  val googlUrl = "https?://goo.gl/([^/?#]+).*".r
  val bufferUrl = "https?://buff.ly/([^/?#]+).*".r
  val dlvritUrl = "https?://dlvr.it/([^/?#]+).*".r
  val twibinUrl = "http://twib.in/l/([^/?#]+).*".r
  val lnkdinUrl = "http://lnkd.in/l/([^/?#]+).*".r

  def getService(url: String, sourceUrl: Option[String] = None)(implicit ec: ExecutionContext): Future[UrlInfo] = getServiceWithResolve(url, None)

  private def getServiceWithResolve(url: String, sourceUrl: Option[String])(implicit ec: ExecutionContext): Future[UrlInfo] = url match {
    case youtube1Url(itemId) =>             Future(UrlInfo.from(url, sourceUrl, Service.Youtube,      Some(itemId)))
    case youtube2Url(itemId) =>             Future(UrlInfo.from(url, sourceUrl, Service.Youtube,      Some(itemId)))
    case dailymotionUrl(itemId) =>          Future(UrlInfo.from(url, sourceUrl, Service.Dailymotion,  Some(itemId)))
    case vimeoUrl(itemId) =>                Future(UrlInfo.from(url, sourceUrl, Service.Vimeo,        Some(itemId)))
    case slideshareUrl(userId, itemId) =>   Future(UrlInfo.from(url, sourceUrl, Service.Slideshare,   Some(itemId), Some(userId)))
    case speakerdeckUrl(userId, itemId) =>  Future(UrlInfo.from(url, sourceUrl, Service.Speakerdeck,  Some(itemId), Some(userId)))
    case slidescomUrl(userId, itemId) =>    Future(UrlInfo.from(url, sourceUrl, Service.Slidescom,    Some(itemId), Some(userId)))
    case preziUrl(itemId) =>                Future(UrlInfo.from(url, sourceUrl, Service.Prezi,        Some(itemId)))
    case googleslidesUrl(itemId) =>         Future(UrlInfo.from(url, sourceUrl, Service.GoogleSlides, Some(itemId)))
    case googledocsUrl(itemId) =>           Future(UrlInfo.from(url, sourceUrl, Service.GoogleDocs,   Some(itemId)))
    case twitterUrl(userId, itemId) =>      Future(UrlInfo.from(url, sourceUrl, Service.Twitter,      Some(itemId)))
    case tcoUrl(_) | owlyUrl(_) | shstUrl(_) | mfttUrl(_) | wpmeUrl(_) | fbmeUrl(_) | bitlyUrl(_) | iftttUrl(_) | googlUrl(_) | bufferUrl(_) | dlvritUrl(_) | twibinUrl(_) | lnkdinUrl(_) =>
      resolve(url).flatMap(resolved => getServiceWithResolve(resolved, Some(sourceUrl.getOrElse(url))))
    case _ => Future(UrlInfo.from(url, sourceUrl, Service.Unknown))
  }

  def resolve(url: String)(implicit ec: ExecutionContext): Future[String] = {
    try {
      WS.url(url).withFollowRedirects(false).head().flatMap { response =>
        response.header("Location").filter(_.startsWith("http")).map { redirect =>
          resolve(redirect).recover {
            case _: Throwable => url
          }
        }.getOrElse {
          Future(url)
        }
      }
    } catch {
      case _: Throwable => Future(url)
    }
  }
}

case class UrlInfo(
  url: String,
  resolvedUrl: String,
  service: UrlInfo.Service.Service,
  category: UrlInfo.Category.Category,
  userId: Option[String] = None,
  itemId: Option[String] = None)
object UrlInfo {
  implicit val formatService = EnumUtils.enumFormat(Service)
  implicit val formatCategory = EnumUtils.enumFormat(Category)
  implicit val format = Json.format[UrlInfo]

  def from(url: String, sourceUrl: Option[String], service: UrlInfo.Service.Service, itemId: Option[String] = None, userId: Option[String] = None) = UrlInfo(
    url = sourceUrl.getOrElse(url),
    resolvedUrl = url,
    service = service,
    category = Category.from(service),
    userId = userId,
    itemId = itemId)

  object Service extends Enumeration {
    type Service = Value
    val
    GooglePlay, iTunes,
    Blogspot,
    GoogleDocs,
    Ebay,
    Slideshare, Speakerdeck, Slidescom, Prezi, GoogleSlides,
    Facebook, Twitter, Instagram, Vine,
    Youtube, Dailymotion, Vimeo,
    Unknown = Value
  }

  object Category extends Enumeration {
    type Category = Value
    val App, Blog, Document, Ecommerce, Press, Slides, Social, Video, Unknown = Value
    def from(service: UrlInfo.Service.Service): UrlInfo.Category.Category = service match {
      case Service.GooglePlay | Service.iTunes => Category.App
      case Service.Blogspot => Category.Blog
      case Service.GoogleDocs => Category.Document
      case Service.Ebay => Category.Ecommerce
      case Service.Slideshare | Service.Speakerdeck | Service.Slidescom | Service.Prezi | Service.GoogleSlides => Category.Slides
      case Service.Facebook | Service.Twitter | Service.Instagram | Service.Vine => Category.Social
      case Service.Youtube | Service.Dailymotion | Service.Vimeo => Category.Video
      case _ => Category.Unknown
    }
  }
}
