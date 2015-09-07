package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class UserRole(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
  def getPriority: Int = UserRole.getPriority(this)
}
object UserRole extends tStringHelper[UserRole] {
  def build(str: String): Either[String, UserRole] = Right(UserRole(str)) // TODO : add validation
  val owner = UserRole("owner")
  val admin = UserRole("admin")
  val member = UserRole("member")
  val guest = UserRole("guest")
  val all = List(owner, admin, member, guest)

  def getPriority(role: UserRole): Int = getPriority(Some(role))
  def getPriority(roleOpt: Option[UserRole]): Int = {
    roleOpt.map { role =>
      val index = all.indexOf(role)
      if (index == -1) all.length + 1 else index + 1
    }.getOrElse(all.length + 2)
  }
}
