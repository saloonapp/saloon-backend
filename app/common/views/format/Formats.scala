package common.views.format

import common.Config
import org.joda.time.{LocalDate, DateTime}
import play.api.i18n.Lang

object Formats {
  def time(date: DateTime, template: String = Config.Application.timeFormat)(implicit lang: Lang): String = {
    date.toString(template, lang.toLocale)
  }
  def date(date: LocalDate, template: String = Config.Application.dateFormat)(implicit lang: Lang): String = {
    date.toString(template, lang.toLocale)
  }
  def datetime(date: DateTime, template: String = Config.Application.datetimeFormat)(implicit lang: Lang): String = {
    date.toString(template, lang.toLocale)
  }
  def period(startOpt: Option[LocalDate], endOpt: Option[LocalDate], template: String = "dd MMMM YYYY", default: String = "N/A")(implicit lang: Lang): String = {
    (startOpt, endOpt) match {
      case (Some(start), Some(end)) if start.isEqual(end) =>  date(start, template)
      case (Some(start), Some(end)) =>                        date(start, "dd") + "-" + date(end, template)
      case (Some(start), None) =>                             date(start, template)
      case (None, Some(end)) =>                               date(end, template)
      case _ =>                                               default
    }
  }
}
