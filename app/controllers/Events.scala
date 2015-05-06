package controllers

import infrastructure.repository.common.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.ExponentRepository
import models.Event
import models.EventUI
import models.EventData
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Events extends Controller {
  val form: Form[EventData] = Form(EventData.fields)
  val repository: Repository[Event] = EventRepository
  val mainRoute = routes.Events
  val viewList = views.html.Application.Events.list
  val viewDetails = views.html.Application.Events.details
  val viewCreate = views.html.Application.Events.create
  val viewUpdate = views.html.Application.Events.update
  def createElt(data: EventData): Event = EventData.toModel(data)
  def toData(elt: Event): EventData = EventData.fromModel(elt)
  def updateElt(elt: Event, data: EventData): Event = EventData.merge(elt, data)
  def successCreateFlash(elt: Event) = s"Event '${elt.name}' has been created"
  def errorCreateFlash(elt: EventData) = s"Event '${elt.name}' can't be created"
  def successUpdateFlash(elt: Event) = s"Event '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Event) = s"Event '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Event) = s"Event '${elt.name}' has been deleted"

  def list(query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, sort.getOrElse("-start")).flatMap { eltPage =>
      if (curPage > 1 && eltPage.totalPages < curPage)
        Future(Redirect(mainRoute.list(query, Some(eltPage.totalPages), sort)))
      else {
        val eltUuids = eltPage.items.map(i => i.uuid)
        for {
          sessionCounts <- Future(Map[String, Int]()) // TODO
          exponentCounts <- ExponentRepository.countForEvents(eltUuids)
        } yield {
          val eltUIPage = eltPage.map { event =>
            EventUI.fromModel(event, sessionCounts.get(event.uuid).getOrElse(0), exponentCounts.get(event.uuid).getOrElse(0))
          }
          Ok(viewList(eltUIPage))
        }
      }
    }
  }

  def create = Action { implicit req =>
    Ok(viewCreate(form))
  }

  def doCreate = Action.async { implicit req =>
    form.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewCreate(formWithErrors))),
      formData => repository.insert(createElt(formData)).map {
        _.map { elt =>
          Redirect(mainRoute.list()).flashing("success" -> successCreateFlash(elt))
        }.getOrElse(InternalServerError(viewCreate(form.fill(formData))).flashing("error" -> errorCreateFlash(formData)))
      })
  }

  def details(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          sessionCount <- Future(0) // TODO
          exponentCount <- ExponentRepository.countForEvent(uuid)
        } yield Ok(viewDetails(EventUI.fromModel(elt, sessionCount, exponentCount)))
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def update(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(viewUpdate(form.fill(toData(elt)), elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  def doUpdate(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        form.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewUpdate(formWithErrors, elt))),
          formData => repository.update(uuid, updateElt(elt, formData)).map {
            _.map { updatedElt =>
              Redirect(mainRoute.details(uuid)).flashing("success" -> successUpdateFlash(updatedElt))
            }.getOrElse(InternalServerError(viewUpdate(form.fill(formData), elt)).flashing("error" -> errorUpdateFlash(elt)))
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def delete(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list()).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }
}
