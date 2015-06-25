package tools.scrapers.innorobo.pages

import scala.collection.JavaConversions._
import org.jsoup.Jsoup

object InnoroboExhibitorListPage {
  def extract(html: String): List[String] = {
    val doc = Jsoup.parse(html)
    doc.select("article.tw_portfolio").toList.map { item =>
      item.select(".portfolio-content h2 a").attr("href")
    }
  }
}
