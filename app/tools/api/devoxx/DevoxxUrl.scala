package tools.api.devoxx

object DevoxxUrl {
  def conferences(cfpUrl: String): String = s"$cfpUrl/conferences"
  def speakers(conferenceUrl: String): String = s"$conferenceUrl/speakers"
  def schedules(conferenceUrl: String): String = s"$conferenceUrl/schedules"
}