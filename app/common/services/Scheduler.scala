package common.services

import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import play.libs.Akka
import scala.concurrent.duration.{FiniteDuration, Duration}
import play.api.libs.concurrent.Execution.Implicits._

object SchedulerHelper {
  def everyWeekAt(weekDay: Int, hour: Int, minutes: Int = 0, seconds: Int = 0)(f: => Unit): DateTime =
    everyAt(nextWeek(new DateTime(), weekDay, hour, minutes, seconds), Duration(7, TimeUnit.DAYS))(f)

  def everyDayAt(hour: Int, minutes: Int = 0, seconds: Int = 0)(f: => Unit): DateTime =
    everyAt(nextDay(new DateTime(), hour, minutes, seconds), Duration(1, TimeUnit.DAYS))(f)

  private def everyAt(next: DateTime, interval: FiniteDuration)(f: => Unit): DateTime = {
    val now = new DateTime()
    Akka.system.scheduler.schedule(FiniteDuration(next.getMillis - now.getMillis, TimeUnit.MILLISECONDS), interval)(f)
    next
  }

  def nextWeek(date: DateTime, weekDay: Int, hour: Int, minutes: Int = 0, seconds: Int = 0): DateTime = {
    def nextDayOfWeek(date: DateTime, weekDay: Int): DateTime = date.plusDays((7 + weekDay - date.getDayOfWeek - 1) % 7 + 1)
    if(date.getDayOfWeek == weekDay && SchedulerHelper.isBeforeTime(date, hour, minutes, seconds)) date.withTime(hour, minutes, seconds, 0)
    else nextDayOfWeek(date, weekDay).withTime(hour, minutes, seconds, 0)
  }
  def nextDay(date: DateTime, hour: Int, minutes: Int = 0, seconds: Int = 0): DateTime = {
    if(SchedulerHelper.isBeforeTime(date, hour, minutes, seconds)) date.withTime(hour, minutes, seconds, 0) else date.plusDays(1).withTime(hour, minutes, seconds, 0)
  }
  def isBeforeTime(date: DateTime, hours: Int, minutes: Int = 0, seconds: Int = 0): Boolean = date.isBefore(date.withTime(hours, minutes, seconds, 0))
  def displayDuration(millis: Long): String = {
    val secs = millis/1000
    val millisRes = millis-(secs*1000)
    val mins = secs/60
    val secsRes = secs-(mins*60)
    val hours = mins/60
    val minsRes = mins-(hours*60)
    val days = hours/24
    val hoursRes = hours-(days*24)
    s"$days days $hoursRes hours $minsRes mins $secsRes secs $millisRes millis"
  }
}
