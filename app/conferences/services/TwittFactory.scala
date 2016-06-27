package conferences.services

import common.Defaults
import conferences.models.Conference
import org.joda.time.{Days, DateTime}

object TwittFactory {
  def newsletterSent(url: String): String = {
    s"Our weekly newsletter about french tech conferences is out : $url by @getSalooN #conf #tech #dév"
  }
  def newConference(c: Conference): String = {
    val url = Defaults.baseUrl+conferences.controllers.routes.Application.detail(c.id)
    val city = c.venue.map(" à "+_.city).getOrElse("")
    val cfp = c.cfp.map(", CFP ouvert jusqu'au "+_.end.toString(Defaults.dateFormatter)).getOrElse("")
    val account = c.twitterAccount.map(" cc @"+_).getOrElse("")
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    val fullTwitt = s"${c.name} aura lieu le ${c.start.toString(Defaults.dateFormatter)}$city$cfp $url$hashtag$account"
    val smallTwitt = s"${c.name} aura lieu le ${c.start.toString(Defaults.dateFormatter)}$city $url$hashtag$account"
    if(fullTwitt.length + 20 - url.length < 140) fullTwitt else smallTwitt
  }
  def publishVideos(c: Conference): String = {
    val url = Defaults.baseUrl+conferences.controllers.routes.Application.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Publications des vidéos de $name $url$hashtag"
  }
  def openCfp(c: Conference): String = {
    val url = Defaults.baseUrl+conferences.controllers.routes.Application.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Ouverture du CFP de $name $url$hashtag"
  }
  def closingCFP(c: Conference): String = {
    val now = new DateTime()
    val url = Defaults.baseUrl+conferences.controllers.routes.Application.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Fermeture du CFP de $name ${dayDuration(now, c.cfp.get.end)} $url$hashtag"
  }
  def startingConference(c: Conference): String = {
    val now = new DateTime()
    val url = Defaults.baseUrl+conferences.controllers.routes.Application.detail(c.id)
    val name = c.twitterAccount.map("@"+_).getOrElse(c.name)
    val hashtag = c.twitterHashtag.map(" #"+_).getOrElse("")
    s"Début de $name ${dayDuration(now, c.start)} $url$hashtag"
  }

  private def dayDuration(start: DateTime, end: DateTime): String = {
    Days.daysBetween(start.toLocalDate(), end.toLocalDate()).getDays() match {
      case 0 => "aujourd'hui"
      case 1 => "demain"
      case 7 => "dans une semaine"
      case 14 => "dans deux semaines"
      case n => s"dans $n jours"
    }
  }
}
