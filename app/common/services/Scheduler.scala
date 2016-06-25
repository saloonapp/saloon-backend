package common.services

import common.Utils
import java.util.concurrent.TimeUnit
import common.repositories.conference.ConferenceRepository
import conferences.models.Conference
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.libs.json.Json
import play.libs.Akka
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._

object Scheduler {
  def init(): Unit = {
    if(Utils.isProd()){ NewsletterScheduler.init() }
  }
}

object NewsletterScheduler {
  def init(): Unit = {
    val now = new DateTime()
    val next = SchedulerHelper.nextWeekDateTime(now, DateTimeConstants.MONDAY, 10)
    play.Logger.info("NewsletterScheduler: next newsletter on "+next)
    Akka.system.scheduler.schedule(Duration(next.getMillis - now.getMillis, TimeUnit.MILLISECONDS), Duration(7, TimeUnit.DAYS))(sendNewsletter)
  }
  def sendNewsletter(): Unit = {
    for {
      (closingCFPs, incomingConferences, newVideos, newCFPs, newConferences) <- getNewsletterInfos(new DateTime())
      url <- MailChimpSrv.createAndSendCampaign(MailChimpCampaign.conferenceListNewsletter(closingCFPs, incomingConferences, newVideos, newCFPs, newConferences))
      // TODO : send twitt : Our weekly newsletter about tech conferences is out : *|URL|* by @getSalooN #event #tech #dév
    } yield {
      play.Logger.info("newsletter sent")
    }
  }
  def getNewsletterInfos(date: DateTime): Future[(List[Conference], List[Conference], List[Conference], List[Conference], List[Conference])] = {
    val closingCFPsFut = ConferenceRepository.find(Json.obj("cfp.end" -> Json.obj("$gt" -> date, "$lt" -> date.plusDays(14))), Json.obj("cfp.end" -> 1))
    val incomingConferencesFut = ConferenceRepository.find(Json.obj("start" -> Json.obj("$gt" -> date, "$lt" -> date.plusDays(7))), Json.obj("start" -> 1))
    val conferencesFut = ConferenceRepository.find()
    val oldConferencesFut = ConferenceRepository.findInPast(date.minusDays(7))
    for {
      closingCFPs <- closingCFPsFut
      incomingConferences <- incomingConferencesFut
      conferences <- conferencesFut
      oldConferences <- oldConferencesFut
    } yield {
      val newVideos = conferences.filter(c => c.videosUrl.isDefined && oldConferences.find(_.id == c.id).map(old => old.videosUrl != c.videosUrl).getOrElse(true))
      val newCFPs = conferences.filter(c => c.cfp.isDefined && oldConferences.find(_.id == c.id).map(old => old.cfp != c.cfp).getOrElse(true))
      val newConferences = conferences.filter(c => oldConferences.find(_.id == c.id).isEmpty)
      (closingCFPs, incomingConferences, newVideos, newCFPs, newConferences)
    }
  }
}

object SchedulerHelper {
  def nextWeekDateTime(date: DateTime, weekDay: Int, hour: Int, minutes: Int = 0, seconds: Int = 0): DateTime = {
    if(date.getDayOfWeek == weekDay && SchedulerHelper.isBeforeTime(date, hour, minutes, seconds)) date.withTime(hour, minutes, seconds, 0)
    else SchedulerHelper.nextDayOfWeek(date, weekDay).withTime(hour, minutes, seconds, 0)
  }
  def nextDayOfWeek(date: DateTime, weekDay: Int): DateTime = date.plusDays((7 + weekDay - date.getDayOfWeek - 1) % 7 + 1)
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
