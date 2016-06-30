package common.views.format

import org.joda.time.DateTime
import play.api.i18n.Lang

object Format {
  def date(date: DateTime, template: String = "dd/MM/yyyy")(implicit lang: Lang): String = {
    date.toString(template, lang.toLocale)
  }
  def time(date: DateTime, template: String = "HH:mm")(implicit lang: Lang): String = {
    date.toString(template, lang.toLocale)
  }
  def datetime(date: DateTime, template: String = "dd/MM/yyyy HH:mm")(implicit lang: Lang): String = {
    date.toString(template, lang.toLocale)
  }
  def period(startOpt: Option[DateTime], endOpt: Option[DateTime], template: String = "dd MMMM YYYY", default: String = "N/A")(implicit lang: Lang): String = {
    endOpt.map { end =>
      startOpt.map { start =>
        if(start.withTimeAtStartOfDay().isEqual(end.withTimeAtStartOfDay())){
          date(start, template)
        } else {
          date(start, "dd")+"-"+date(end, template)
        }
      }.getOrElse {
        date(end, template)
      }
    }.getOrElse {
      startOpt.map(date(_, template)).getOrElse(default)
    }
  }
}
