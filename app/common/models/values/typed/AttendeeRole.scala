package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class AttendeeRole(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object AttendeeRole extends tStringHelper[AttendeeRole] {
  def build(str: String): Either[String, AttendeeRole] = Right(AttendeeRole(str)) // TODO : add validation
  val staff = AttendeeRole("staff")
  val exposant = AttendeeRole("exposant")
  val speaker = AttendeeRole("speaker")
  val visiteur = AttendeeRole("visiteur")
  val all = List(staff, exposant, speaker, visiteur)
}
