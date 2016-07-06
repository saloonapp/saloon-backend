package common.services

import common.Utils
import common.Defaults
import java.util.concurrent.TimeUnit
import common.models.values.typed.{TextHTML, Email}
import common.repositories.conference.ConferenceRepository
import conferences.models.Conference
import conferences.services.TwittFactory
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.libs.json.Json
import play.libs.Akka
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, Duration}
import play.api.libs.concurrent.Execution.Implicits._

object Scheduler {
  def init(): Unit = {
    if(Utils.isProd()){
      NewsletterScheduler.init()
      TwittScheduler.init()
      TestScheduler.init()
    }
  }
}

object TestScheduler {
  def init(): Unit = {
    val next1 = SchedulerHelper.everyDayAt(23)(test1)
    val next2 = SchedulerHelper.everyDayAt(5)(test2)
    play.Logger.info("TestScheduler: next1 "+next1)
    play.Logger.info("TestScheduler: next2 "+next2)
  }
  def test1(): Unit = {
    val content = TextHTML("Hello from TestScheduler 1 (run at 23h00 every day)")
    EmailSrv.sendEmail(EmailData("TestScheduler 1", Defaults.adminEmail, Email("loicknuchel@gmail.com"), "TEST 1", content, content.toPlainText))
  }
  def test2(): Unit = {
    val content = TextHTML("Hello from TestScheduler 2 (run at 05h00 every day)")
    EmailSrv.sendEmail(EmailData("TestScheduler 2", Defaults.adminEmail, Email("loicknuchel@gmail.com"), "TEST 2", content, content.toPlainText))
  }
}

object NewsletterScheduler {
  def init(): Unit = {
    val next = SchedulerHelper.everyWeekAt(DateTimeConstants.MONDAY, 10)(sendNewsletter)
    play.Logger.info("NewsletterScheduler: next newsletter on "+next)
  }
  def sendNewsletter(): Unit = {
    play.Logger.info("NewsletterScheduler.sendNewsletter()")
    getNewsletterInfos(new DateTime()).map { case (closingCFPs, incomingConferences, newData) =>
      if(closingCFPs.length + incomingConferences.length + newData.length > 0) {
        MailChimpSrv.createAndSendCampaign(MailChimpCampaign.conferenceListNewsletter(closingCFPs, incomingConferences, newData)).map { url =>
          TwitterSrv.twitt(TwittFactory.newsletterSent(url))
          play.Logger.info("newsletter sent")
        }
      }
    }
  }
  def getNewsletterInfos(date: DateTime): Future[(List[Conference], List[Conference], List[(Conference, Map[String, Boolean])])] = {
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
      val newData = conferences.filterNot(c => closingCFPs.find(_.id == c.id).isDefined || incomingConferences.find(_.id == c.id).isDefined).map { c =>
        (c, Map(
          "created" -> oldConferences.find(_.id == c.id).isEmpty,
          "videos" -> (c.videosUrl.isDefined && oldConferences.find(_.id == c.id).map(old => old.videosUrl != c.videosUrl).getOrElse(true)),
          "cfp" -> (c.cfp.isDefined && oldConferences.find(_.id == c.id).map(old => old.cfp != c.cfp).getOrElse(true))
        ))
      }.filter(_._2.map(_._2).foldLeft(false)(_ || _)) // has at least one 'true'
      (closingCFPs, incomingConferences, newData)
    }
  }
}

object TwittScheduler {
  def init(): Unit = {
    val next = SchedulerHelper.everyDayAt(10)(sendTwitts)
    play.Logger.info("TwittScheduler: next twitts on "+next)
  }
  def sendTwitts(): Unit = {
    play.Logger.info("TwittScheduler.sendTwitts()")
    getTwitts(new DateTime()).map { twitts =>
      play.Logger.info(if(twitts.length > 0) twitts.length+" twitts à envoyer :" else "aucun twitt à envoyer")
      twitts.map(t => play.Logger.info("  - "+t))
      twitts.map(TwitterSrv.twitt)
    }
  }
  def getTwitts(date: DateTime): Future[List[String]] = {
    val today = date.withTime(0, 0, 0, 0)
    val nearClosingCFPsFut = ConferenceRepository.find(Json.obj("$or" -> Json.arr(
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(1))), // cfp closes tomorrow
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(3))), // cfp closes in 3 days
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(7))), // cfp closes in 1 week
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(14))) // cfp closes in 2 weeks
    )))
    val nearStartingConfsFut = ConferenceRepository.find(Json.obj("$or" -> Json.arr(
      Json.obj("start" -> Json.obj("$eq" -> today)), // conf starts today
      Json.obj("start" -> Json.obj("$eq" -> today.plusDays(1))), // conf starts tomorrow
      Json.obj("start" -> Json.obj("$eq" -> today.plusDays(7))) // conf starts in 1 week
    )))
    for {
      nearClosingCFPs <- nearClosingCFPsFut
      nearStartingConfs <- nearStartingConfsFut
    } yield {
      val twitts = nearClosingCFPs.map { c =>
        (TwittFactory.closingCFP(c, today), c.cfp.get.end.getMillis)
      } ++ nearStartingConfs.map { c =>
        (TwittFactory.startingConference(c, today), c.start.getMillis)
      }
      twitts.sortBy(_._2).map(_._1)
    }
  }
}

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
