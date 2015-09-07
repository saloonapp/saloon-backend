package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class RequestStatus(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object RequestStatus extends tStringHelper[RequestStatus] {
  def build(str: String): Either[String, RequestStatus] = Right(RequestStatus(str)) // TODO : add validation
  val pending = RequestStatus("pending")
  val accepted = RequestStatus("accepted")
  val rejected = RequestStatus("rejected")
  val canceled = RequestStatus("canceled")
  val all = List(pending, accepted, rejected, canceled)
}
