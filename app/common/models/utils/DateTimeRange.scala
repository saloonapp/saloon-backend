package common.models.utils

import common.Config
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data.validation.{Valid, ValidationError, Invalid, Constraint}
import play.api.data.{Mapping, FormError}
import play.api.data.format.Formatter

case class DateTimeRange(
  start: DateTime,
  end: DateTime)
object DateTimeRange {
  private val errKey = "error.datetimerange"
  private val regex = s"(${Config.Application.datetimeFormat}) - (${Config.Application.datetimeFormat})".replaceAll("[a-zA-Z]", "\\\\d").r
  private def fromString(str: String): Either[String, DateTimeRange] = str.trim match {
    case regex(start, end) => Right(DateTimeRange(DateTime.parse(start, Config.Application.datetimeFormatter), DateTime.parse(end, Config.Application.datetimeFormatter)))
    case _ => Left(errKey)
  }
  private val formMapping = new Formatter[DateTimeRange] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateTimeRange] =
      data.get(key).map { value => fromString(value).left.map(msg => Seq(FormError(key, msg, Nil))) }.getOrElse(Left(Seq(FormError(key, errKey, Nil))))
    override def unbind(key: String, value: DateTimeRange): Map[String, String] =
      Map(key -> (value.start.toString(Config.Application.datetimeFormat)+" - "+value.end.toString(Config.Application.datetimeFormat)))
  }

  // ex: https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/api/data/Forms.scala
  val mapping: Mapping[DateTimeRange] = of[DateTimeRange](formMapping)
  // ex: https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/api/data/validation/Validation.scala
  object Constraints {
    def required: Constraint[DateTimeRange] = Constraint[DateTimeRange]("constraint.required") { o =>
      if (o == null) Invalid(ValidationError("error.required")) else Valid
    }
  }
}
