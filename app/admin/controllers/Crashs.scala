package admin.controllers

import infrastructure.repository.CrashRepository
import common.models.user.Crash
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

object Crashs extends Controller {

  def list = Action.async { implicit req =>
    CrashRepository.find(Json.obj("solved" -> Json.obj("$exists" -> false))).map { crashJsons =>
      val crashs = crashJsons.map(_.asOpt[Crash]).flatten
      val malformedCrashs = crashJsons.map(json => if (json.asOpt[Crash].isEmpty) Some(json) else None).flatten
      Ok(admin.views.html.Crashs.list(crashs, malformedCrashs))
    }
  }

  def details(uuid: String) = Action.async { implicit req =>
    CrashRepository.get(uuid).flatMap { crashOpt =>
      crashOpt.flatMap(_.asOpt[Crash]).map { crash =>
        for {
          similarCrashs <- CrashRepository.find(Json.obj("error" -> (crashOpt.get \ "error"))).map(_.map(_.asOpt[Crash]).flatten.filter(_.uuid != crash.uuid))
          previousCrashs <- getRecursiveCrashList(crash, getPreviousCrash)
        } yield {
          Ok(admin.views.html.Crashs.details(crash, previousCrashs, similarCrashs))
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def solved(uuid: String) = Action.async { implicit req =>
    CrashRepository.get(uuid).flatMap { crashOpt =>
      crashOpt.map { crash =>
        CrashRepository.markAsSolved(Json.obj("error" -> crash \ "error")).map { err =>
          if (err.ok) Redirect(routes.Crashs.list()).flashing("success" -> s"Crashs marked as solved !")
          else Redirect(routes.Crashs.details(uuid)).flashing("error" -> err.errMsg.getOrElse(err.message))
        }
      }.getOrElse(Future(Redirect(routes.Crashs.list()).flashing("error" -> s"Unable to find crash $uuid")))
    }
  }

  private def getPreviousCrash(crash: Crash): Future[Option[Crash]] = {
    crash.previousClientId.map { previousClientId =>
      CrashRepository.get(Json.obj("clientId" -> previousClientId)).map(_.flatMap(_.asOpt[Crash]))
    }.getOrElse {
      Future(None)
    }
  }
  private def getNextCrash(crash: Crash): Future[Option[Crash]] = {
    CrashRepository.get(Json.obj("previousClientId" -> crash.clientId)).map(_.flatMap(_.asOpt[Crash]))
  }
  private def getRecursiveCrashList(c: Crash, getCrash: (Crash) => Future[Option[Crash]], max: Int = 5, previousCrashs: List[Crash] = List()): Future[List[Crash]] = {
    if (max > 0) {
      getCrash(c).flatMap {
        _.map { previousCrash =>
          getRecursiveCrashList(previousCrash, getCrash, max - 1, List(previousCrash) ++ previousCrashs)
        }.getOrElse {
          Future(previousCrashs)
        }
      }
    } else {
      Future(previousCrashs)
    }
  }
}
