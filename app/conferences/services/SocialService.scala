package conferences.services

import common.repositories.conference.ConferenceRepository
import common.services.TwitterSrv
import org.joda.time.DateTime
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SocialService {
  def sendTwitts(): Future[Boolean] = {
    play.Logger.info("SocialService.sendTwitts()")
    getTwitts(new DateTime()).map { twitts =>
      play.Logger.info(if(twitts.length > 0) twitts.length+" twitts à envoyer :" else "aucun twitt à envoyer")
      twitts.map(t => play.Logger.info("  - "+t))
      TwitterSrv.sendTwitts(twitts, 10)
      true
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
