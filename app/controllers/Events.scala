package controllers

import common.FileBodyParser
import common.models.Page
import models._
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
import play.api.libs.json._

object Events extends Controller {
  val form: Form[EventData] = Form(EventData.fields)
  val fileImportForm = Form(FileImportConfig.fields)
  val urlImportForm = Form(UrlImportConfig.fields)
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
        for {
          actions <- EventSrv.getActions(uuid)
          eltUI <- EventSrv.addMetadata(elt)
        } yield {
          Ok(viewDetails(eltUI, actions))
        }
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def stats(uuid: String) = Action.async { implicit req =>
    EventSrv.getActions(uuid).map { actions =>
      val filename = actions.head.event.name + "_stats.csv"
      val content = FileExporter.makeCsv(actions.map(_.toMap))
      Ok(content)
        .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
        .as("text/csv")
    }
  }

  def update(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(viewUpdate(form.fill(toData(elt)), elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  def refresh(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap { eltOpt =>
      eltOpt.map {
        _.refreshUrl.map { url =>
          val getData = for {
            remoteSourceOpt <- EventSrv.fetchFullEvent(url)
            localEventOpt <- EventRepository.getByUuid(uuid)
            localSessions <- SessionRepository.findByEvent(uuid)
            localExponents <- ExponentRepository.findByEvent(uuid)
          } yield (remoteSourceOpt, localEventOpt, localSessions, localExponents)

          getData.flatMap {
            case (remoteSourceOpt, localEventOpt, localSessions, localExponents) =>
              (for {
                (remoteEvent, remoteSessions, remoteExponents) <- remoteSourceOpt
                localEvent <- localEventOpt
              } yield {
                val updatedEvent = localEvent.merge(remoteEvent)
                val (createdSessions, updatedSessions, deletedSessions) = diff(localSessions, remoteSessions, (s: models.Session) => s.source.map(_.ref).getOrElse(""), (os: models.Session, ns: models.Session) => os.merge(ns))
                val (createdExponents, updatedExponents, deletedExponents) = diff(localExponents, remoteExponents, (e: Exponent) => e.source.map(_.ref).getOrElse(""), (oe: Exponent, ne: Exponent) => oe.merge(ne))

                for {
                  eventUpdated <- EventRepository.update(uuid, updatedEvent)
                  sessionsCreated <- SessionRepository.bulkInsert(createdSessions.map(_.copy(eventId = uuid)))
                  sessionsUpdated <- SessionRepository.bulkUpdate(updatedSessions.map(s => (s.uuid, s)))
                  sessionsDeleted <- SessionRepository.bulkDelete(deletedSessions.map(_.uuid))
                  exponentsCreated <- ExponentRepository.bulkInsert(createdExponents.map(_.copy(eventId = uuid)))
                  exponentsUpdated <- ExponentRepository.bulkUpdate(updatedExponents.map(e => (e.uuid, e)))
                  exponentsDeleted <- ExponentRepository.bulkDelete(deletedExponents.map(_.uuid))
                } yield {
                  Redirect(mainRoute.details(uuid)).flashing("success" ->
                    (s"${localEvent.name} updated :" +
                      s"<br>- $sessionsCreated sessions created<br>- $sessionsUpdated sessions updated<br>- ${deletedSessions.length} sessions deleted" +
                      s"<br>- $exponentsCreated exponents created<br>- $exponentsUpdated exponents updated<br>- ${deletedExponents.length} exponents deleted"))
                }
              }).getOrElse(Future(Ok(s"Result of url $url is incorrect !")))
          }
        }.getOrElse(Future(BadRequest(views.html.error(s"Event $uuid doesn't have a refreshUrl !"))))
      }.getOrElse(Future(NotFound(views.html.error404(s"Unable to find event $uuid !"))))
    }
  }

  private def diff[A](oldElts: List[A], newElts: List[A], getRef: A => String, merge: (A, A) => A): (List[A], List[A], List[A]) = {
    val createdElts = newElts.filter(ne => oldElts.find(oe => getRef(oe) == getRef(ne)).isEmpty)
    val deletedElts = oldElts.filter(oe => newElts.find(ne => getRef(oe) == getRef(ne)).isEmpty)
    val updatedElts = oldElts
      .map(oe => newElts.find(ne => getRef(oe) == getRef(ne)).map(ne => (oe, ne))).flatten
      .map { case (oe, ne) => merge(oe, ne) }
    (createdElts, updatedElts, deletedElts)
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
    Ok(viewOps(fileImportForm, urlImportForm.fill(UrlImportConfig("", true, false))))
  }

  def fileImport = Action.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    fileImportForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewOps(formWithErrors, urlImportForm))),
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
        }.getOrElse(Future(BadRequest(viewOps(fileImportForm.fill(formData), urlImportForm)).flashing("error" -> "You must import a file !")))
      })
  }

  val urlParser = """https?://(.+\.herokuapp.com)/events/([0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12})""".r
  def urlImport = Action.async { implicit req =>
    urlImportForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewOps(fileImportForm, formWithErrors))),
      formData => {
        formData.url match {
          case urlParser(remoteHost, eventId) => {
            EventSrv.fetchEvent(remoteHost, eventId, formData.newIds).flatMap {
              _.map {
                case (event, sessions, exponents) =>
                  EventRepository.getByUuid(event.uuid).flatMap { oldEventOpt =>
                    if (oldEventOpt.isDefined) {
                      if (formData.replaceIds) {
                        EventRepository.delete(event.uuid).flatMap { opt =>
                          EventSrv.insertAll(event, sessions, exponents).map { insertedOpt =>
                            if (insertedOpt.isDefined) { Redirect(mainRoute.list()).flashing("success" -> s"L'événement <b>${event.name}</b> bien mis à jour") }
                            else { InternalServerError(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"Erreur pendant la mise à jour de ${event.name} (id: ${event.uuid})"))) }
                          }
                        }
                      } else {
                        Future(BadRequest(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"L'événement <b>${event.uuid}</b> existe déjà. Créez de nouveaux ids ou permettez de supprimer l'événement existant."))))
                      }
                    } else {
                      EventSrv.insertAll(event, sessions, exponents).map { insertedOpt =>
                        if (insertedOpt.isDefined) { Redirect(mainRoute.list()).flashing("success" -> s"L'événement <b>${event.name}</b> bien créé") }
                        else { InternalServerError(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"Erreur pendant la création de ${event.name} (id: ${event.uuid})"))) }
                      }
                    }
                  }
              }.getOrElse(Future(BadRequest(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"L'événement <b>$eventId</b> n'existe pas sur <b>$remoteHost</b>")))))
            }
          }
          case _ => Future(BadRequest(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"L'url <b>${formData.url}</b> ne correspond pas au format attendu..."))))
        }
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
