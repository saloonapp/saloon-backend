package tools.scrapers.meta

import tools.scrapers.meta.models.SiteMetas
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.libs.ws._
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object MetaScraper {
  def get(url: String) = {
    getSafe(url).map {
      _.map { response =>
        val doc = Jsoup.parse(response.body)
        val metas = getMetas(doc) ++ getInfos(doc)
        val normalizedMetas = metas.map(normalizeMetaAttrs).groupBy(_._1).map {
          case (key, values) => (key, values.map(_._2))
        }
        SiteMetas.build(url, normalizedMetas)
      }
    }
  }

  private def normalizeMetaAttrs(attrs: Map[String, String]): (String, String) = {
    if (attrs.get("charset").isDefined) {
      ("charset", attrs.get("charset").get)
    } else {
      val name = attrs.get("name").orElse(attrs.get("property")).orElse(attrs.get("itemprop")).orElse(attrs.get("id")).orElse(attrs.get("http-equiv")).getOrElse("unknown")
      val value = attrs.get("content").getOrElse("")
      (name, value)
    }
  }

  private def getInfos(doc: Document): List[Map[String, String]] = {
    val title = doc.select("head title").text()
    val icons = doc.select("link[rel~=icon]").toList.map { icon =>
      Map("name" -> "icon", "content" -> icon.attr("href"))
    }
    val url = doc.select("link[rel=canonical]").attr("href")

    List(
      Map("name" -> "title", "content" -> title),
      Map("name" -> "url", "content" -> url)) ++ icons
  }

  private def getMetas(doc: Document): List[Map[String, String]] = {
    doc.select("meta").toList.map { elt =>
      getAttrs(elt)
    }
  }

  private val possibleAttrs = List("name", "property", "itemprop", "http-equiv", "content", "charset", "id", "data-app")
  private def getAttrs(elt: Element): Map[String, String] = {
    possibleAttrs.map { attr =>
      val value = elt.attr(attr)
      if (value.isEmpty) None else Some((attr, value))
    }.flatten.toMap ++ Map("original" -> elt.toString)
  }

  private def getSafe(url: String): Option[Future[WSResponse]] = {
    try {
      Some(WS.url(url).get())
    } catch {
      case e: NullPointerException => None
    }
  }
}