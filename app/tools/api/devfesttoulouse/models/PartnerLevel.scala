package tools.api.devfesttoulouse.models

import play.api.libs.json.Json

case class Partner(
                    name: String,
                    url: String,
                    logoUrl: String,
                    width: Option[Int],
                    sourceUrl: Option[String]
                  )
case class PartnerLevel(
                         title: String,
                         logos: List[Partner]
                       )
object PartnerLevel {
  implicit val formatPartner = Json.format[Partner]
  implicit val format = Json.format[PartnerLevel]
}
