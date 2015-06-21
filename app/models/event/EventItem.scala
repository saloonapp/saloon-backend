package models.event

trait EventItem {
  val uuid: String
  val name: String
  def getType(): String = {
    this match {
      case _: Event => Event.className
      case _: Session => Session.className
      case _: Exponent => Exponent.className
      case _ => "Unknown"
    }
  }
}
