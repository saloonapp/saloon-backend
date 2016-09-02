package conferences.services

import common.Config
import common.services.SimpleTweet
import conferences.models.Conference
import org.joda.time.{Days, LocalDate}

object TwittFactory {
  def newsletterSent(url: String): String = {
    s"La newsletter des conférences tech françaises est là : $url via @getSalooN #conf #tech #dév"
  }
  def newConference(c: Conference): String = {
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val city = c.location.flatMap(_.locality).map(" à "+_).getOrElse("")
    val cfp = c.cfp.map(", CFP ouvert jusqu'au "+_.end.toString(Config.Application.dateFormatter)).getOrElse("")
    val account = c.twitterAccount.map(" cc @"+_).getOrElse("")
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    val fullTwitt = s"${c.name} aura lieu le ${c.start.toString(Config.Application.dateFormatter)}$city$cfp $url$hashtag$account"
    val smallTwitt = s"${c.name} aura lieu le ${c.start.toString(Config.Application.dateFormatter)}$city $url$hashtag$account"
    if(fullTwitt.length + 20 - url.length < 140) fullTwitt else smallTwitt
  }
  def publishVideos(c: Conference): String = {
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Publication des vidéos de $name $url$hashtag"
  }
  def openCfp(c: Conference): String = {
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Ouverture du CFP de $name $url$hashtag"
  }
  def closingCFP(c: Conference, now: LocalDate): String = {
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Fermeture du CFP de $name ${dayDuration(now, c.cfp.get.end)} $url$hashtag"
  }
  def startingConference(c: Conference, now: LocalDate): String = {
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Début de $name ${dayDuration(now, c.start)} $url$hashtag"
  }

  def genericRemindToAddSlides(c: Conference): String = { // à 8h30, 12h, 16h et 19h pendant la conf
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val account = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Speaker à $account ? Publie tes slides sur $url$hashtag"
  }
  def replySuggestToAddSlides(c: Conference, tweet: SimpleTweet): String = {
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val userMention = tweet.user.map("@"+_.screen_name).getOrElse("")
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    tweet.user.flatMap(_.lang) match {
      case Some("fr") => s"$userMention N'hésites pas à ajouter tes slides sur $url$hashtag"
      case _ => s"$userMention Feel free to add your slides to $url$hashtag"
    }
  }
  /*def publishSlides(c: Conference): String = { // le soir
    val url = Config.Application.baseUrl + conferences.controllers.routes.Conferences.detail(c.id)
    val account = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Des présentationss (slides et vidéos) ont été publiées pour $account $url$hashtag"
  }*/

  private def dayDuration(start: LocalDate, end: LocalDate): String = {
    Days.daysBetween(start, end).getDays() match {
      case 0 => "aujourd'hui"
      case 1 => "demain"
      case 7 => "dans une semaine"
      case 14 => "dans deux semaines"
      case n => s"dans $n jours"
    }
  }
}
