package controllers

import common.FileBodyParser
import common.models.Page
import models.Event
import models.EventUI
import models.EventData
import models.ImportConfig
import services.FileImporter
import services.FileExporter
import services.EventSrv
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Events extends Controller {
  val form: Form[EventData] = Form(EventData.fields)
  val importForm = Form(ImportConfig.fields)
  val repository: Repository[Event] = EventRepository
  val mainRoute = routes.Events
  val viewList = views.html.Application.Events.list
  val viewDetails = views.html.Application.Events.details
  val viewCreate = views.html.Application.Events.create
  val viewUpdate = views.html.Application.Events.update
  val viewOps = views.html.Application.Events.operations
  def createElt(data: EventData): Event = EventData.toModel(data)
  def toData(elt: Event): EventData = EventData.fromModel(elt)
  def updateElt(elt: Event, data: EventData): Event = EventData.merge(elt, data)
  def successCreateFlash(elt: Event) = s"Event '${elt.name}' has been created"
  def errorCreateFlash(elt: EventData) = s"Event '${elt.name}' can't be created"
  def successUpdateFlash(elt: Event) = s"Event '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Event) = s"Event '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Event) = s"Event '${elt.name}' has been deleted"
  def successImportFlash(count: Int) = s"${count} events imported"

  def list(query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("-start")).flatMap { eltPage =>
      if (curPage > 1 && eltPage.totalPages < curPage)
        Future(Redirect(mainRoute.list(query, Some(eltPage.totalPages), pageSize, sort)))
      else
        eltPage.batchMapAsync(EventSrv.addMetadata _).map { eltUIPage => Ok(viewList(eltUIPage)) }
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
        EventSrv.addMetadata(elt).map { eltUI => Ok(viewDetails(eltUI)) }
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

  def operations = Action { implicit req =>
    Ok(viewOps(importForm))
  }

  def fileImport = Action.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    importForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewOps(formWithErrors))),
      formData => {
        req.body.file("importedFile").map { filePart =>
          val reader = new java.io.StringReader(new String(filePart.ref))
          FileImporter.importEvents(reader, formData).map {
            case (nbInserted, errors) =>
              Redirect(mainRoute.list())
                .flashing(
                  "success" -> successImportFlash(nbInserted),
                  "error" -> (if (errors.isEmpty) { "" } else { "Errors: <br>" + errors.map("- " + _.toString).mkString("<br>") }))
          }
        }.getOrElse(Future(BadRequest(viewOps(importForm.fill(formData))).flashing("error" -> "You must import a file !")))
      })
  }

  def fileExport = Action.async { implicit req =>
    EventRepository.findAll().map { elts =>
      val filename = "events.csv"
      val content = FileExporter.makeCsv(elts.map(_.toMap))
      Ok(content)
        .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
        .as("text/csv")
    }
  }
}
