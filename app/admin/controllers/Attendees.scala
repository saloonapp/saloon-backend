package admin.controllers

import common.FileBodyParser
import common.models.utils.Page
import common.models.FileImportConfig
import common.models.event.Attendee
import common.models.event.AttendeeData
import common.services.FileImporter
import common.services.FileExporter
import common.repositories.Repository
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Attendees extends SilhouetteEnvironment {
  val form: Form[AttendeeData] = Form(AttendeeData.fields)
  val fileImportForm = Form(FileImportConfig.fields)
  val repository: Repository[Attendee] = AttendeeRepository
  val mainRoute = routes.Attendees
  val viewList = admin.views.html.Attendees.list
  val viewDetails = admin.views.html.Attendees.details
  val viewCreate = admin.views.html.Attendees.create
  val viewUpdate = admin.views.html.Attendees.update
  def createElt(data: AttendeeData): Attendee = AttendeeData.toModel(data)
  def toData(elt: Attendee): AttendeeData = AttendeeData.fromModel(elt)
  def updateElt(elt: Attendee, data: AttendeeData): Attendee = AttendeeData.merge(elt, data)
  def successCreateFlash(elt: Attendee) = s"Attendee '${elt.name}' has been created"
  def errorCreateFlash(elt: AttendeeData) = s"Attendee '${elt.name}' can't be created"
  def successUpdateFlash(elt: Attendee) = s"Attendee '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Attendee) = s"Attendee '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Attendee) = s"Attendee '${elt.name}' has been deleted"
  def successImportFlash(count: Int) = s"${count} attendees imported"

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    val curPage = page.getOrElse(1)
    for {
      eltPage <- AttendeeRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name"))
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage)
        Redirect(mainRoute.list(eventId, query, Some(eltPage.totalPages), pageSize, sort))
      else
        eventOpt
          .map { event => Ok(viewList(eltPage, event)) }
          .getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).map { eventOpt =>
      eventOpt
        .map { event => Ok(viewCreate(form, event)) }
        .getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        form.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewCreate(formWithErrors, event))),
          formData => repository.insert(createElt(formData)).map {
            _.map { elt =>
              Redirect(mainRoute.list(eventId)).flashing("success" -> successCreateFlash(elt))
            }.getOrElse(InternalServerError(viewCreate(form.fill(formData), event)).flashing("error" -> errorCreateFlash(formData)))
          })
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(viewDetails(elt, event)) }
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def update(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(viewUpdate(form.fill(toData(elt)), elt, event)) }
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doUpdate(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    val dataFuture = for {
      eltOpt <- repository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    dataFuture.flatMap { data =>
      data._1.flatMap { elt =>
        data._2.map { event =>
          form.bindFromRequest.fold(
            formWithErrors => Future(BadRequest(viewUpdate(formWithErrors, elt, event))),
            formData => repository.update(uuid, updateElt(elt, formData)).map {
              _.map { updatedElt =>
                Redirect(mainRoute.list(eventId)).flashing("success" -> successUpdateFlash(updatedElt))
              }.getOrElse(InternalServerError(viewUpdate(form.fill(formData), elt, event)).flashing("error" -> errorUpdateFlash(elt)))
            })
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def delete(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list(eventId)).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  // TODO : add preview of updates
  def fileImport(eventId: String) = SecuredAction.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        fileImportForm.bindFromRequest.fold(
          formWithErrors => Future(Redirect(routes.Events.operations(eventId)).flashing("error" -> "Form error...")),
          formData => {
            req.body.file("importedFile").map { filePart =>
              val reader = new java.io.StringReader(new String(filePart.ref, formData.encoding))
              FileImporter.importAttendees(reader, formData, eventId).map {
                case (nbInserted, errors) =>
                  Redirect(routes.Events.details(eventId))
                    .flashing(
                      "success" -> successImportFlash(nbInserted),
                      "error" -> (if (errors.isEmpty) { "" } else { "Errors: <br>" + errors.map("- " + _.toString).mkString("<br>") }))
              }
            }.getOrElse(Future(Redirect(routes.Events.operations(eventId)).flashing("error" -> "You must import a file !")))
          })
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def fileExport(eventId: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        AttendeeRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_attendees.csv"
          val content = FileExporter.makeCsv(elts.map(_.toMap))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }
}
