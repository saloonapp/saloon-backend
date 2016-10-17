package tools.api.devfesttoulouse

object DevFestUrl {
  val baseUrl = "https://devfesttoulouse.fr"
  def speakers(conferenceUrl: String): String = s"$conferenceUrl/data/speakers.json"
  def sessions(conferenceUrl: String): String = s"$conferenceUrl/data/sessions.json"
  def schedules(conferenceUrl: String): String = s"$conferenceUrl/data/schedule.json"
}
