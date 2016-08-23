package common.services

import common.EnumUtils
import common.services.UrlInfo.Service
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}

object Urls {
  /* URLS TO ADD
    - newspaper
      - https://www.theguardian.com/technology/2016/aug/06/nsa-zero-days-stockpile-security-vulnerability-defcon
    - blog
      - http://cookingwithamandaz.blogspot.fr/2016/08/13-reasons-to-never-purchase-snacks.html?utm_source=twitterfeed&utm_medium=twitter
    - ecommerce
      - http://deals.ebay.com/5003492080_Vince_Camuto_Shelly_1_Women__Open_Toe_Synthetic__Slides_Sandal?rmvSB=true
    - other
      - https://www.dropbox.com/s/3b08ae2m9owat66/Resolution%20Revolution%20Slideshow.pdf?dl=0
   */

  /* WRONG RESOLVE
    - http://tech.mg/E3zIme
   */
  private val hash = "([^/?&#]+)"
  val googlePlay = s"https?://play.google.com/store/apps/details?id=$hash.*".r
  val iTunes = s"https?://itunes.apple.com/us/app/$hash/$hash.*".r

  val youtube1 = "https?://www.youtube.com/watch\\?(?:[^=]+=[^&]+&)*v=([^&]+).*".r
  val youtube2 = s"https?://youtu.be/$hash.*".r
  val dailymotion = s"https?://www.dailymotion.com/video/$hash.*".r
  val vimeo = s"https?://vimeo.com/$hash.*".r
  val infoq = s"https?://www.infoq.com/presentations/$hash".r

  val slideshare = s"https?://[a-z]+.slideshare.net/$hash/$hash.*".r
  val speakerdeck = s"https?://speakerdeck.com/$hash/$hash.*".r
  val slidescom = s"https?://slides.com/$hash/$hash.*".r
  val prezi = s"https?://prezi.com/p/$hash.*".r
  val googleslides = s"https?://docs.google.com/presentation/d/$hash.*".r

  val facebookUser = s"https?://www.facebook.com/$hash.*".r
  val facebookPhoto = s"https?://www.facebook.com/photo.php?fbid=$hash.*".r
  val facebookVideo1 = s"https?://www.facebook.com/video.php?v=$hash.*".r
  val facebookVideo2 = s"https?://www.facebook.com/$hash/videos/$hash.*".r
  val facebookPost = s"https?://www.facebook.com/$hash/posts/$hash.*".r
  val twitter = s"https?://twitter.com/$hash/status/$hash.*".r
  val instagram = s"https?://www.instagram.com/p/$hash.*".r
  val vine = s"https?://vine.co/v/$hash.*".r

  val googledocs = s"https?://drive.google.com/file/d/$hash.*".r

  val tco = s"https?://t.co/$hash.*".r
  val owly = s"https?://ow.ly/$hash.*".r
  val shst = s"https?://sh.st/$hash.*".r
  val mftt = s"https?://mf.tt/$hash.*".r
  val wpme = s"https?://wp.me/$hash.*".r
  val fbme = s"https?://fb.me/$hash.*".r
  val bitly = s"https?://bit.ly/$hash.*".r
  val ifttt = s"https?://ift.tt/$hash.*".r
  val googl = s"https?://goo.gl/$hash.*".r
  val buffer = s"https?://buff.ly/$hash.*".r
  val dlvrit = s"https?://dlvr.it/$hash.*".r
  val twibin = s"http://twib.in/l/$hash.*".r
  val lnkdin = s"http://lnkd.in/l/$hash.*".r
}

object UrlSrv {
  def getService(url: String, sourceUrl: Option[String] = None)(implicit ec: ExecutionContext): Future[UrlInfo] = getServiceWithResolve(url, None)

  private def getServiceWithResolve(url: String, sourceUrl: Option[String])(implicit ec: ExecutionContext): Future[UrlInfo] = url match {
    case Urls.iTunes(userId, itemId) =>         Future(UrlInfo.from(url, sourceUrl, Service.iTunes,       Some(itemId), Some(userId)))
    case Urls.googlePlay(itemId) =>             Future(UrlInfo.from(url, sourceUrl, Service.GooglePlay,   Some(itemId)))

    case Urls.youtube1(itemId) =>               Future(UrlInfo.from(url, sourceUrl, Service.Youtube,      Some(itemId)))
    case Urls.youtube2(itemId) =>               Future(UrlInfo.from(url, sourceUrl, Service.Youtube,      Some(itemId)))
    case Urls.dailymotion(itemId) =>            Future(UrlInfo.from(url, sourceUrl, Service.Dailymotion,  Some(itemId)))
    case Urls.vimeo(itemId) =>                  Future(UrlInfo.from(url, sourceUrl, Service.Vimeo,        Some(itemId)))
    case Urls.infoq(itemId) =>                  Future(UrlInfo.from(url, sourceUrl, Service.InfoQ,        Some(itemId)))

    case Urls.slideshare(userId, itemId) =>     Future(UrlInfo.from(url, sourceUrl, Service.Slideshare,   Some(itemId), Some(userId)))
    case Urls.speakerdeck(userId, itemId) =>    Future(UrlInfo.from(url, sourceUrl, Service.Speakerdeck,  Some(itemId), Some(userId)))
    case Urls.slidescom(userId, itemId) =>      Future(UrlInfo.from(url, sourceUrl, Service.Slidescom,    Some(itemId), Some(userId)))
    case Urls.prezi(itemId) =>                  Future(UrlInfo.from(url, sourceUrl, Service.Prezi,        Some(itemId)))
    case Urls.googleslides(itemId) =>           Future(UrlInfo.from(url, sourceUrl, Service.GoogleSlides, Some(itemId)))

    case Urls.facebookUser(itemId) =>           Future(UrlInfo.from(url, sourceUrl, Service.Facebook,     Some(itemId)))
    case Urls.facebookPhoto(itemId) =>          Future(UrlInfo.from(url, sourceUrl, Service.Facebook,     Some(itemId)))
    case Urls.facebookVideo1(itemId) =>         Future(UrlInfo.from(url, sourceUrl, Service.Facebook,     Some(itemId)))
    case Urls.facebookVideo2(userId, itemId) => Future(UrlInfo.from(url, sourceUrl, Service.Facebook,     Some(itemId), Some(userId)))
    case Urls.facebookPost(userId, itemId) =>   Future(UrlInfo.from(url, sourceUrl, Service.Facebook,     Some(itemId), Some(userId)))
    case Urls.twitter(userId, itemId) =>        Future(UrlInfo.from(url, sourceUrl, Service.Twitter,      Some(itemId), Some(userId)))
    case Urls.instagram(itemId) =>              Future(UrlInfo.from(url, sourceUrl, Service.Instagram,    Some(itemId)))
    case Urls.vine(itemId) =>                   Future(UrlInfo.from(url, sourceUrl, Service.Vine,         Some(itemId)))

    case Urls.googledocs(itemId) =>             Future(UrlInfo.from(url, sourceUrl, Service.GoogleDocs,   Some(itemId)))

    case Urls.tco(_) | Urls.owly(_) | Urls.shst(_) | Urls.mftt(_) | Urls.wpme(_) | Urls.fbme(_) | Urls.bitly(_) | Urls.ifttt(_) | Urls.googl(_) | Urls.buffer(_) |
         Urls.dlvrit(_) | Urls.twibin(_) | Urls.lnkdin(_) =>
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
    Youtube, Dailymotion, Vimeo, InfoQ,
    Unknown = Value
  }

  object Category extends Enumeration {
    type Category = Value
    val App, Blog, Document, Ecommerce, Press, Slides, Social, Video, Unknown = Value
    def from(service: UrlInfo.Service.Service): UrlInfo.Category.Category = service match {
      case Service.iTunes | Service.GooglePlay => Category.App
      case Service.Blogspot => Category.Blog
      case Service.GoogleDocs => Category.Document
      case Service.Ebay => Category.Ecommerce
      case Service.Slideshare | Service.Speakerdeck | Service.Slidescom | Service.Prezi | Service.GoogleSlides => Category.Slides
      case Service.Facebook | Service.Twitter | Service.Instagram | Service.Vine => Category.Social
      case Service.Youtube | Service.Dailymotion | Service.Vimeo | Service.InfoQ => Category.Video
      case _ => Category.Unknown
    }
  }
}
