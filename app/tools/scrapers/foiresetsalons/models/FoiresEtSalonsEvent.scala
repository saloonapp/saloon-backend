package tools.scrapers.foiresetsalons.models

import common.Utils
import common.models.values.Source
import common.models.event.GenericEvent
import common.models.event.GenericEventInfo
import common.models.event.GenericEventVenue
import common.models.event.GenericEventOrganizer
import common.models.event.GenericEventAddress
import common.models.event.GenericEventStats
import play.api.libs.json.Json
import org.joda.time.DateTime

case class FoiresEtSalonsAddress(
  name: String,
  street: String,
  city: String) {
  def toGeneric: GenericEventAddress = GenericEventAddress(this.name, "", this.street, "", this.city, "")
}
object FoiresEtSalonsAddress {
  implicit val format = Json.format[FoiresEtSalonsAddress]
  def build(list: List[String]): FoiresEtSalonsAddress =
    if (list.length == 0) FoiresEtSalonsAddress("", "", "")
    else if (list.length == 1) FoiresEtSalonsAddress("", "", list(0))
    else if (list.length == 2) FoiresEtSalonsAddress("", list(0), list(1))
    else FoiresEtSalonsAddress(list(0), list(1), list(2))
}
case class FoiresEtSalonsStats(
  area: Int,
  venues: Int,
  exponents: Int,
  visitors: Int,
  certified: String) {
  def toGeneric: GenericEventStats = GenericEventStats(None, intOpt(area), intOpt(exponents), intOpt(venues), intOpt(visitors))
  private def intOpt(v: Int): Option[Int] = if (v > 0) Some(v) else None
}
object FoiresEtSalonsStats {
  implicit val format = Json.format[FoiresEtSalonsStats]
}
case class FoiresEtSalonsOrga(
  name: String,
  sigle: String,
  address: FoiresEtSalonsAddress,
  phone: String,
  site: String) {
  def toGeneric: GenericEventOrganizer = GenericEventOrganizer("", this.name, this.address.toGeneric, Utils.toOpt(this.site), None, Utils.toOpt(this.phone))
}
object FoiresEtSalonsOrga {
  implicit val format = Json.format[FoiresEtSalonsOrga]
}
case class FoiresEtSalonsEvent(
  name: String,
  address: FoiresEtSalonsAddress,
  category: String,
  access: List[String],
  start: Option[DateTime],
  end: Option[DateTime],
  sectors: List[String],
  products: List[String],
  stats: FoiresEtSalonsStats,
  orga: FoiresEtSalonsOrga,
  url: String)
object FoiresEtSalonsEvent {
  implicit val format = Json.format[FoiresEtSalonsEvent]

  val sourceName = "FoiresEtSalonsScraper"
  def toGenericEvent(event: FoiresEtSalonsEvent): GenericEvent = {
    GenericEvent(
      List(Source(getRef(event.url), sourceName, event.url)),
      "", // uuid
      event.name,
      GenericEventInfo(
        "", // logo
        event.start,
        event.end,
        "", // decription
        "", // decriptionHTML
        Some(GenericEventVenue("", "", event.address.toGeneric, None, None, None)),
        List(event.orga.toGeneric),
        None, // website
        None, // email
        None), // phone
      event.products, // tags
      Map(), // socialUrls
      event.stats.toGeneric,
      GenericEvent.Status.draft,
      List(), // attendees
      List(), // exponents
      List(), // sessions
      Map(), // exponentTeam
      Map()) // sessionSpeakers
  }

  private val refRegex = "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=([0-9]+)&decl=([0-9]+)".r
  private def getRef(url: String): String = url match {
    case refRegex(manif, decl) => s"$manif-$decl"
    case _ => url
  }
}
