package models

trait EventItem {
  val uuid: String
  val name: String
  def getType(): String = {
    this match {
      case _: Event => EventUI.className
      case _: Session => SessionUI.className
      case _: Exponent => ExponentUI.className
      case _ => "Unknown"
    }
  }
}
