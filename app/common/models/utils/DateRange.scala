package common.models.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.FormError
import play.api.data.format.Formatter

case class DateRange(
  start: DateTime,
  end: DateTime)
object DateRange {
  val errKey = "error.daterange"
  val dateFormat = "dd/MM/yyyy"
  val dateFormatter = DateTimeFormat.forPattern(dateFormat)
  val regex = s"($dateFormat) - ($dateFormat)".replaceAll("[a-zA-Z]", "\\\\d").r
  def fromString(str: String): Either[String, DateRange] = str.trim match {
    case regex(start, end) => Right(DateRange(DateTime.parse(start, dateFormatter), DateTime.parse(end, dateFormatter)))
    case _ => Left(errKey)
  }
  implicit val formMapping = new Formatter[DateRange] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateRange] =
      data.get(key).map { value => fromString(value).left.map(msg => Seq(FormError(key, msg, Nil))) }.getOrElse(Left(Seq(FormError(key, errKey, Nil))))
    override def unbind(key: String, value: DateRange): Map[String, String] =
      Map(key -> (value.start.toString(dateFormat)+" - "+value.end.toString(dateFormat)))
  }
}
