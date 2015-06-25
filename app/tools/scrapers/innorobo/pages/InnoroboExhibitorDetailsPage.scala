package tools.scrapers.innorobo.pages

import tools.scrapers.innorobo.models.InnoroboExhibitor
import scala.collection.JavaConversions._
import org.jsoup.Jsoup

object InnoroboExhibitorDetailsPage {
  def extract(html: String, url: String): InnoroboExhibitor = {
    val doc = Jsoup.parse(html)
    val name = doc.select("h1.page-title").text()
    val exhibitor = doc.select(".single-portfolio")(0)
    val tabs = exhibitor.select(".tab-pane")
    val company = if (tabs.length > 0) tabs(0).text() else ""
    val logo = exhibitor.select(".tw-title-container + p").select("img").attr("src")
    val website = if (tabs.length > 2) tabs(2).select("a").attr("href") else ""
    val categories = if (tabs.length > 2) tabs(2).select("img").map { c => (c.attr("alt"), c.attr("src")) } else List()
    val description = company + "\n\n" + categories.map { case (alt, src) => s"![$alt]($src)" }.mkString(" ")
    InnoroboExhibitor(name, description, logo, website, url)
  }
}
