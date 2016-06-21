package common.models.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.Forms.of
import play.api.data.{Mapping, FormError}
import play.api.data.format.Formatter
import play.api.data.validation.{Valid, ValidationError, Invalid, Constraint}

case class DateRange(
  start: DateTime,
  end: DateTime)
object DateRange {
  private val errKey = "error.daterange"
  private val dateFormat = "dd/MM/yyyy"
  private val dateFormatter = DateTimeFormat.forPattern(dateFormat)
  private val regex = s"($dateFormat) - ($dateFormat)".replaceAll("[a-zA-Z]", "\\\\d").r
  private def fromString(str: String): Either[String, DateRange] = str.trim match {
    case regex(start, end) => Right(DateRange(DateTime.parse(start, dateFormatter), DateTime.parse(end, dateFormatter)))
    case _ => Left(errKey)
  }
  private val formMapping = new Formatter[DateRange] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateRange] =
      data.get(key).map { value => fromString(value).left.map(msg => Seq(FormError(key, msg, Nil))) }.getOrElse(Left(Seq(FormError(key, errKey, Nil))))
    override def unbind(key: String, value: DateRange): Map[String, String] =
      Map(key -> (value.start.toString(dateFormat)+" - "+value.end.toString(dateFormat)))
  }

  // ex: https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/api/data/Forms.scala
  val mapping: Mapping[DateRange] = of[DateRange](formMapping)
  // ex: https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/api/data/validation/Validation.scala
  object Constraints {
    def required: Constraint[DateRange] = Constraint[DateRange]("constraint.required") { o =>
      if (o == null) Invalid(ValidationError("error.required")) else Valid
    }
  }
}
