package tools.scrapers.foiresetsalons.pages

import tools.scrapers.foiresetsalons.FESScraper
import tools.scrapers.foiresetsalons.models.FESEventItem
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object FESListPage {
  val dateParser = "du ([0-9]+/[0-9]+/[0-9]+) au ([0-9]+/[0-9]+/[0-9]+).*".r

  def extract(html: String, page: String): List[FESEventItem] = {
    val doc = Jsoup.parse(html)
    val table = doc.select("form table").get(if (page == "catalogue") 1 else 2)
    table.select("tr").toList.drop(1).map { item =>
      val cells = item.select("td")
      val (start, end) = cells.get(2).text() match {
        case dateParser(start, end) => (start, end)
      }
      FESEventItem(
        FESScraper.baseUrl + "/" + cells.get(0).select("a").attr("href"),
        cells.get(0).select("a").text(),
        cells.get(1).text(),
        start,
        end,
        cells.get(3).text().split(" - ").toList)
    }
  }
}
