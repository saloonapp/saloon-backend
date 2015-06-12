package controllers

import common.FileBodyParser
import common.models.Page
import models._
import models.UserAction._
import services.FileImporter
import services.FileExporter
import services.EventSrv
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.UserActionRepository
import scala.util.Try
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

  def report(eventId: String, userId: String) = Action.async { implicit req =>
    UserActionRepository.findByUserEvent(userId, eventId).flatMap { actions =>
      val subscribeOpt = actions.find(_.action.isSubscribe())
      subscribeOpt.map {
        _.action match {
          case subscribe: SubscribeUserAction => {
            val favoriteSessionUuids = actions.filter(a => a.action.isFavorite() && a.itemType == SessionUI.className).map(_.itemId)
            val favoriteExponentUuids = actions.filter(a => a.action.isFavorite() && a.itemType == ExponentUI.className).map(_.itemId)
            for {
              event <- EventRepository.getByUuid(eventId)
              sessions <- if (subscribe.filter == "favorite") SessionRepository.findByUuids(favoriteSessionUuids) else SessionRepository.findByEvent(eventId)
              exponents <- if (subscribe.filter == "favorite") ExponentRepository.findByUuids(favoriteExponentUuids) else ExponentRepository.findByEvent(eventId)
            } yield {
              Ok(views.html.Mail.eventAttendeeReport(event.get, sessions, exponents, actions, subscribe.filter))
            }
          }
          case _ => Future(NotFound(views.html.error(s"User $userId didn't subscribe to event $eventId")))
        }
      }.getOrElse(Future(NotFound(views.html.error(s"User $userId didn't subscribe to event $eventId"))))
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
    Ok(viewOps(fileImportForm, urlImportForm.fill(UrlImportConfig())))
  }

  def fileImport = Action.async(FileBodyParser.multipartFormDataAsBytes) { implicit req =>
    fileImportForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewOps(formWithErrors, urlImportForm))),
      formData => {
        req.body.file("importedFile").map { filePart =>
          val reader = new java.io.StringReader(new String(filePart.ref, formData.encoding))
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

  def urlImport = Action.async { implicit req =>
    urlImportForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewOps(fileImportForm, formWithErrors))),
      formData => {
        val eventUrl = EventSrv.formatUrl(formData.url)
        EventSrv.fetchFullEvent(eventUrl).flatMap {
          _.map {
            case (remoteEvent, remoteSessions, remoteExponents) =>
              val getData = for {
                localEventOpt <- EventRepository.getByUuid(remoteEvent.uuid)
                localSessions <- SessionRepository.findByEvent(remoteEvent.uuid)
                localExponents <- ExponentRepository.findByEvent(remoteEvent.uuid)
              } yield (localEventOpt, localSessions, localExponents)

              getData.flatMap {
                case (localEventOpt, localSessions, localExponents) =>
                  localEventOpt.map { localEvent =>
                    val remoteSource = Json.obj("event" -> remoteEvent, "sessions" -> remoteSessions, "exponents" -> remoteExponents)
                    val updatedEvent = localEvent.merge(remoteEvent)
                    val (createdSessions, deletedSessions, updatedSessions) = EventSrv.sessionDiff(localSessions, remoteSessions)
                    val (createdExponents, deletedExponents, updatedExponents) = EventSrv.exponentDiff(localExponents, remoteExponents)
                    Future(Ok(views.html.Application.Events.refresh(localEvent, updatedEvent, createdSessions, deletedSessions, updatedSessions, createdExponents, deletedExponents, updatedExponents, remoteSource)))
                  }.getOrElse {
                    EventSrv.insertAll(remoteEvent, remoteSessions, remoteExponents).map { insertedOpt =>
                      if (insertedOpt.isDefined) { Redirect(mainRoute.list()).flashing("success" -> s"L'événement <b>${remoteEvent.name}</b> bien créé") }
                      else { InternalServerError(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"Erreur pendant la création de ${remoteEvent.name} (id: ${remoteEvent.uuid})"))) }
                    }
                  }
              }
          }.getOrElse(Future(BadRequest(viewOps(fileImportForm, urlImportForm.fill(formData))(req.flash + ("error" -> s"Aucun événement ne correspond à l'url <b>$eventUrl</b>")))))
        }
      })
  }

  def refresh(uuid: String) = Action.async { implicit req =>
    EventRepository.getByUuid(uuid).flatMap { eventOpt =>
      eventOpt.map { localEvent =>
        localEvent.refreshUrl.map { url =>
          for {
            remoteSourceOpt <- EventSrv.fetchFullEvent(url)
            localSessions <- SessionRepository.findByEvent(uuid)
            localExponents <- ExponentRepository.findByEvent(uuid)
          } yield {
            remoteSourceOpt.map {
              case (remoteEvent, remoteSessions, remoteExponents) =>
                val remoteSource = Json.obj("event" -> remoteEvent, "sessions" -> remoteSessions, "exponents" -> remoteExponents)
                val updatedEvent = localEvent.merge(remoteEvent)
                val (createdSessions, deletedSessions, updatedSessions) = EventSrv.sessionDiff(localSessions, remoteSessions)
                val (createdExponents, deletedExponents, updatedExponents) = EventSrv.exponentDiff(localExponents, remoteExponents)
                Ok(views.html.Application.Events.refresh(localEvent, updatedEvent, createdSessions, deletedSessions, updatedSessions, createdExponents, deletedExponents, updatedExponents, remoteSource))
            }.getOrElse(Ok(s"Result of url $url is incorrect !"))
          }
        }.getOrElse(Future(BadRequest(views.html.error(s"Event $uuid doesn't have a refreshUrl !"))))
      }.getOrElse(Future(NotFound(views.html.error404(s"Unable to find event $uuid !"))))
    }
  }

  def doRefresh(uuid: String) = Action.async { implicit req =>
    req.body.asFormUrlEncoded.flatMap(_.get("data").flatMap(_.headOption)).map { data =>
      val jsonTry: Try[JsValue] = Try(Json.parse(data))
      (for {
        remoteEvent <- jsonTry.map(json => (json \ "event").asOpt[Event]).getOrElse(None)
        remoteSessions <- jsonTry.map(json => (json \ "sessions").asOpt[List[models.Session]]).getOrElse(None)
        remoteExponents <- jsonTry.map(json => (json \ "exponents").asOpt[List[Exponent]]).getOrElse(None)
      } yield {
        val getData = for {
          localEventOpt <- EventRepository.getByUuid(uuid)
          localSessions <- SessionRepository.findByEvent(uuid)
          localExponents <- ExponentRepository.findByEvent(uuid)
        } yield (localEventOpt, localSessions, localExponents)

        getData.flatMap {
          case (localEventOpt, localSessions, localExponents) =>
            localEventOpt.map { localEvent =>
              val updatedEvent = localEvent.merge(remoteEvent)
              val (createdSessions, deletedSessions, updatedSessions) = EventSrv.sessionDiff(localSessions, remoteSessions)
              val (createdExponents, deletedExponents, updatedExponents) = EventSrv.exponentDiff(localExponents, remoteExponents)

              for {
                eventUpdated <- EventRepository.update(localEvent.uuid, updatedEvent)
                sessionsCreated <- SessionRepository.bulkInsert(createdSessions.map(_.copy(eventId = localEvent.uuid)))
                sessionsDeleted <- SessionRepository.bulkDelete(deletedSessions.map(_.uuid))
                sessionsUpdated <- SessionRepository.bulkUpdate(updatedSessions.map(s => (s._2.uuid, s._2)))
                exponentsCreated <- ExponentRepository.bulkInsert(createdExponents.map(_.copy(eventId = localEvent.uuid)))
                exponentsDeleted <- ExponentRepository.bulkDelete(deletedExponents.map(_.uuid))
                exponentsUpdated <- ExponentRepository.bulkUpdate(updatedExponents.map(e => (e._2.uuid, e._2)))
              } yield {
                Redirect(mainRoute.details(uuid)).flashing("success" ->
                  (s"${localEvent.name} updated :" +
                    s"<br>- $sessionsCreated sessions created<br>- $sessionsUpdated sessions updated<br>- ${deletedSessions.length} sessions deleted" +
                    s"<br>- $exponentsCreated exponents created<br>- $exponentsUpdated exponents updated<br>- ${deletedExponents.length} exponents deleted"))
              }
            }.getOrElse(Future(Redirect(mainRoute.list()).flashing("error" -> s"L'événement $uuid est introuvable ! Impossible de le mettre à jour...")))
        }
      }).getOrElse(Future(Redirect(mainRoute.refresh(uuid)).flashing("error" -> "Les données de l'attribut 'data' du body ne sont pas correctes (JSON: {event: {}, sessions: [], exponents: []})!")))
    }.getOrElse(Future(Redirect(mainRoute.refresh(uuid)).flashing("error" -> "Le body doit contenir les données dans un attribut 'data' !")))
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
