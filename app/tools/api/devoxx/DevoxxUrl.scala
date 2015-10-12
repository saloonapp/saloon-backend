package tools.api.devoxx

object DevoxxUrl {
  def conferences(cfpUrl: String): String = s"$cfpUrl/conferences"
  def speakers(conferenceUrl: String): String = s"$conferenceUrl/speakers"
  def schedules(conferenceUrl: String): String = s"$conferenceUrl/schedules"

  val speakerUrlRegex = "(https?://[^/]+)/api/conferences/([^/]+)/speakers/([0-9a-f]{40})".r
  def speakerIdFromUrl(speakerUrl: String): Option[String] = speakerUrl match {
    case speakerUrlRegex(_, _, id) => Some(id)
    case _ => None
  }
}