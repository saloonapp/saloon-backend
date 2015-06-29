import play.api._
import play.api.i18n.Lang
import play.api.mvc.EssentialAction
import play.api.mvc.RequestHeader
import play.api.mvc.Flash
import play.api.mvc.Results.Unauthorized
import play.api.mvc.Results.Forbidden
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTimeZone
import com.mohiva.play.silhouette.core.SecuredSettings

object Global extends GlobalSettings with SecuredSettings {
  override def doFilter(action: EssentialAction): EssentialAction = EssentialAction { request =>
    action.apply(request).map(_.withHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, userId, timestamp",
      // cache access control response for one day
      "Access-Control-Max-Age" -> (60 * 60 * 24).toString))
  }

  override def onStart(app: Application) {
    DateTimeZone.setDefault(DateTimeZone.forID("Europe/Paris"))
  }

  override def onNotAuthenticated(request: RequestHeader, lang: Lang) = {
    implicit val flash = Flash()
    implicit val header = request
    Some(Future.successful(Unauthorized(admin.views.html.error("Authentication required !"))))
  }

  override def onNotAuthorized(request: RequestHeader, lang: Lang) = {
    implicit val flash = Flash()
    implicit val header = request
    Some(Future.successful(Forbidden(admin.views.html.error("Not authorized !"))))
  }
}
