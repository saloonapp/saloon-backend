package admin.controllers

import common.Utils
import common.FileBodyParser
import common.models.utils.Page
import common.models.FileImportConfig
import common.models.UrlImportConfig
import common.models.event.Event
import common.models.event.EventData
import common.models.event.Session
import common.models.event.Exponent
import common.models.user.SubscribeUserAction
import common.services.FileImporter
import common.services.FileExporter
import common.services.EventSrv
import common.services.EmailSrv
import common.services.MandrillSrv
import common.repositories.Repository
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import common.repositories.user.UserActionRepository
import common.repositories.event.EventRepository
import authentication.environments.SilhouetteEnvironment
import api.controllers.compatibility.Writer
import scala.util.Try
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Events extends SilhouetteEnvironment {
  val form: Form[EventData] = Form(EventData.fields)
  val fileImportForm = Form(FileImportConfig.fields)
  val urlImportForm = Form(UrlImportConfig.fields)
  val repository: Repository[Event] = EventRepository
  val mainRoute = routes.Events
  val viewList = admin.views.html.Events.list
  val viewDetails = admin.views.html.Events.details
  val viewCreate = admin.views.html.Events.create
  val viewUpdate = admin.views.html.Events.update
  val viewOps = admin.views.html.Events.operations
  def createElt(data: EventData): Event = EventData.toModel(data)
  def toData(elt: Event): EventData = EventData.fromModel(elt)
  def updateElt(elt: Event, data: EventData): Event = EventData.merge(elt, data)
  def successCreateFlash(elt: Event) = s"Event '${elt.name}' has been created"
  def errorCreateFlash(elt: EventData) = s"Event '${elt.name}' can't be created"
  def successUpdateFlash(elt: Event) = s"Event '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Event) = s"Event '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Event) = s"Event '${elt.name}' has been deleted"
  def successImportFlash(count: Int) = s"${count} events imported"

  def list(query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("-info.start")).flatMap { eltPage =>
      if (curPage > 1 && eltPage.totalPages < curPage)
        Future(Redirect(mainRoute.list(query, Some(eltPage.totalPages), pageSize, sort)))
      else
        eltPage.batchMapAsync(EventSrv.addMetadata _).map { eltUIPage => Ok(viewList(eltUIPage)) }
    }
  }

  def create = SecuredAction.async { implicit req =>
    EventRepository.getCategories().map { categories =>
      Ok(viewCreate(form, categories))
    }
  }

  def doCreate = SecuredAction.async { implicit req =>
    form.bindFromRequest.fold(
      formWithErrors => EventRepository.getCategories().map { categories => BadRequest(viewCreate(formWithErrors, categories)) },
      formData => repository.insert(createElt(formData)).flatMap {
        _.map { elt =>
          Future(Redirect(mainRoute.list()).flashing("success" -> successCreateFlash(elt)))
        }.getOrElse {
          EventRepository.getCategories().map { categories => InternalServerError(viewCreate(form.fill(formData), categories)).flashing("error" -> errorCreateFlash(formData)) }
        }
      })
  }

  def doCreateFromUrl = SecuredAction.async { implicit req =>
    Utils.getFormParam("url").map { url =>
      val eventUrl = EventSrv.formatUrl(url)
      EventSrv.fetchFullEvent(eventUrl).flatMap {
        _.map {
          case (event, attendees, sessions, exponents) =>
            EventRepository.getByUuid(event.uuid).flatMap {
              _.map { localEvent =>
                Future(Redirect(mainRoute.create()).flashing("error" -> s"L'événement ${event.uuid} existe déjà !"))
              }.getOrElse {
                EventSrv.insertAll(event, attendees, sessions, exponents).map {
                  _.map {
                    case (event, attendeeCount, sessionCount, exponentCount) =>
                      Redirect(mainRoute.details(event.uuid)).flashing("success" -> s"Evénément ${event.name} créé avec $attendeeCount attendees, $sessionCount sessions, $exponentCount exponents")
                  }.getOrElse {
                    Redirect(mainRoute.create()).flashing("error" -> s"Problème lors de la création de l'événément ${event.name} (${event.uuid})")
                  }
                }
              }
            }
        }.getOrElse(Future(Redirect(mainRoute.create()).flashing("error" -> s"Pas d'événement trouvé à l'url : <b>$eventUrl</b>")))
      }
    }.getOrElse(Future(Redirect(mainRoute.create()).flashing("error" -> "Le champ 'url' est nécessaire !")))
  }

  def details(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          actions <- EventSrv.getActions(uuid)
          (elt, attendeeCount, sessionCount, exponentCount, actionCount) <- EventSrv.addMetadata(elt)
        } yield {
          Ok(viewDetails(elt, attendeeCount, sessionCount, exponentCount, actionCount, actions))
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def report(eventId: String, userId: String) = SecuredAction.async { implicit req =>
    EmailSrv.generateEventReport(eventId, userId).map {
      _.map { email =>
        Ok(play.twirl.api.Html(email.html))
      }.getOrElse(NotFound(admin.views.html.error(s"User $userId didn't subscribe to event $eventId")))
    }
  }

  def sendReport(eventId: String, userId: String) = SecuredAction.async { implicit req =>
    UserActionRepository.getSubscribe(userId, Event.className, eventId).flatMap {
      _.map {
        _.action match {
          case SubscribeUserAction(email, filter, subscribe) => {
            EmailSrv.generateEventReport(eventId, userId).flatMap {
              _.map { emailData =>
                MandrillSrv.sendEmail(emailData.copy(to = email)).map { res =>
                  Redirect(mainRoute.operations(eventId)).flashing("success" -> s"Email envoyé : ${Json.stringify(res)}")
                }
              }.getOrElse(Future(Redirect(mainRoute.operations(eventId)).flashing("error" -> s"User $userId didn't subscribe to event $eventId")))
            }
          }
          case _ => Future(Redirect(mainRoute.operations(eventId)).flashing("error" -> s"User $userId didn't subscribe to event $eventId"))
        }
      }.getOrElse(Future(Redirect(mainRoute.operations(eventId)).flashing("error" -> s"User $userId didn't subscribe to event $eventId")))
    }
  }

  def reportsPreview(eventId: String) = SecuredAction.async { implicit req =>
    UserActionRepository.findSubscribes(Event.className, eventId).map { subscribes =>
      var users = subscribes.map(s => s.action match {
        case sub: SubscribeUserAction => Some((s.userId, sub))
        case _ => None
      }).flatten
      Ok(admin.views.html.Events.reportsPreview(eventId, users))
    }
  }

  def sendReports(eventId: String) = SecuredAction.async { implicit req =>
    UserActionRepository.findSubscribes(Event.className, eventId).flatMap { subscribes =>
      var users = subscribes.map(s => s.action match {
        case sub: SubscribeUserAction => Some((s.userId, sub))
        case _ => None
      }).flatten

      val listFutures = users.map {
        case (userId, sub) =>
          EmailSrv.generateEventReport(eventId, userId).flatMap {
            _.map { emailData => MandrillSrv.sendEmail(emailData.copy(to = sub.email)) }.getOrElse(Future(Json.obj("message" -> s"error for ${sub.email}")))
          }
      }

      Future.sequence(listFutures).map { results =>
        val message = "Emails envoyés :<br>" + results.map(r => s" - ${Json.stringify(r)}<br>").mkString("")
        Redirect(mainRoute.operations(eventId)).flashing("success" -> message)
      }
    }
  }

  def stats(uuid: String) = SecuredAction.async { implicit req =>
    EventSrv.getActions(uuid).map { actions =>
      val filename = actions.head.event.name + "_stats.csv"
      val content = FileExporter.makeCsv(actions.map(_.toMap))
      Ok(content)
        .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
        .as("text/csv")
    }
  }

  def update(uuid: String) = SecuredAction.async { implicit req =>
    for {
      eltOpt <- repository.getByUuid(uuid)
      categories <- EventRepository.getCategories()
    } yield {
      eltOpt.map { elt =>
        Ok(viewUpdate(form.fill(toData(elt)), elt, categories))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doUpdate(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        form.bindFromRequest.fold(
          formWithErrors => EventRepository.getCategories().map { categories => BadRequest(viewUpdate(formWithErrors, elt, categories)) },
          formData => repository.update(uuid, updateElt(elt, formData)).flatMap {
            _.map { updatedElt =>
              Future(Redirect(mainRoute.details(uuid)).flashing("success" -> successUpdateFlash(updatedElt)))
            }.getOrElse {
              EventRepository.getCategories().map { categories => InternalServerError(viewUpdate(form.fill(formData), elt, categories)).flashing("error" -> errorUpdateFlash(elt)) }
            }
          })
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def delete(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list()).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def operations(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { event =>
        Ok(viewOps(event, urlImportForm, fileImportForm.fill(FileImportConfig())))
      }.getOrElse(Redirect(mainRoute.list()).flashing("error" -> s"Event $uuid not found..."))
    }
  }

  def urlImport(uuid: String) = SecuredAction.async { implicit req =>
    urlImportForm.bindFromRequest.fold(
      formWithErrors => repository.getByUuid(uuid).map {
        _.map { event =>
          BadRequest(viewOps(event, formWithErrors, fileImportForm))
        }.getOrElse(Redirect(mainRoute.list()).flashing("error" -> s"Event $uuid not found..."))
      },
      formData => {
        val eventUrl = EventSrv.formatUrl(formData.url)
        val getData = for {
          fullEventOpt <- EventSrv.fetchFullEvent(eventUrl)
          localEventOpt <- EventRepository.getByUuid(uuid)
        } yield (fullEventOpt, localEventOpt)

        getData.flatMap {
          case (fullEventOpt, localEventOpt) =>
            (for {
              (remoteEvent, remoteAttendees, remoteSessions, remoteExponents) <- fullEventOpt
              localEvent <- localEventOpt
            } yield {
              val getData = for {
                localAttendees <- AttendeeRepository.findByEvent(remoteEvent.uuid)
                localSessions <- SessionRepository.findByEvent(remoteEvent.uuid)
                localExponents <- ExponentRepository.findByEvent(remoteEvent.uuid)
              } yield (localEventOpt, localAttendees, localSessions, localExponents)

              getData.flatMap {
                case (localEventOpt, localAttendees, localSessions, localExponents) =>
                  localEventOpt.map { localEvent =>
                    val remoteSource = Writer.write(remoteEvent, remoteAttendees, remoteSessions, remoteExponents, Writer.lastVersion)
                    val updatedEvent = localEvent.merge(remoteEvent)
                    val (createdAttendees, deletedAttendees, updatedAttendees) = EventSrv.attendeeDiff(localAttendees, remoteAttendees)
                    val (createdSessions, deletedSessions, updatedSessions) = EventSrv.sessionDiff(localSessions, remoteSessions)
                    val (createdExponents, deletedExponents, updatedExponents) = EventSrv.exponentDiff(localExponents, remoteExponents)
                    Future(Ok(admin.views.html.Events.refresh(localEvent, updatedEvent, createdAttendees, deletedAttendees, updatedAttendees, createdSessions, deletedSessions, updatedSessions, createdExponents, deletedExponents, updatedExponents, remoteSource)))
                  }.getOrElse {
                    EventSrv.insertAll(remoteEvent, remoteAttendees, remoteSessions, remoteExponents).map { insertedOpt =>
                      if (insertedOpt.isDefined) { Redirect(mainRoute.list()).flashing("success" -> s"L'événement <b>${remoteEvent.name}</b> bien créé") }
                      else { InternalServerError(viewOps(localEvent, urlImportForm.fill(formData), fileImportForm)(req.flash + ("error" -> s"Erreur pendant la création de ${remoteEvent.name} (id: ${remoteEvent.uuid})"))) }
                    }
                  }
              }
            }).getOrElse(Future(Redirect(mainRoute.list()).flashing("error" -> s"Event $uuid not found...")))
        }
      })
  }

  def refresh(uuid: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(uuid).flatMap { eventOpt =>
      eventOpt.map { localEvent =>
        localEvent.meta.refreshUrl.map { url =>
          val eventUrl = EventSrv.formatUrl(url)
          for {
            remoteSourceOpt <- EventSrv.fetchFullEvent(eventUrl)
            localAttendees <- AttendeeRepository.findByEvent(uuid)
            localSessions <- SessionRepository.findByEvent(uuid)
            localExponents <- ExponentRepository.findByEvent(uuid)
          } yield {
            remoteSourceOpt.map {
              case (remoteEvent, remoteAttendees, remoteSessions, remoteExponents) =>
                val remoteSource = Writer.write(remoteEvent, remoteAttendees, remoteSessions, remoteExponents, Writer.lastVersion)
                val updatedEvent = localEvent.merge(remoteEvent)
                val (createdAttendees, deletedAttendees, updatedAttendees) = EventSrv.attendeeDiff(localAttendees, remoteAttendees)
                val (createdSessions, deletedSessions, updatedSessions) = EventSrv.sessionDiff(localSessions, remoteSessions)
                val (createdExponents, deletedExponents, updatedExponents) = EventSrv.exponentDiff(localExponents, remoteExponents)
                Ok(admin.views.html.Events.refresh(localEvent, updatedEvent, createdAttendees, deletedAttendees, updatedAttendees, createdSessions, deletedSessions, updatedSessions, createdExponents, deletedExponents, updatedExponents, remoteSource))
            }.getOrElse(Ok(s"Result of url $url is incorrect !"))
          }
        }.getOrElse(Future(BadRequest(admin.views.html.error(s"Event $uuid doesn't have a refreshUrl !"))))
      }.getOrElse(Future(NotFound(admin.views.html.error404(s"Unable to find event $uuid !"))))
    }
  }

  def doRefresh(uuid: String) = SecuredAction.async { implicit req =>
    req.body.asFormUrlEncoded.flatMap(_.get("data").flatMap(_.headOption)).map { data =>
      val jsonTry: Try[JsValue] = Try(Json.parse(data))
      (for {
        remoteEvent <- jsonTry.map(json => (json \ "event").asOpt[Event]).getOrElse(None)
        remoteSessions <- jsonTry.map(json => (json \ "sessions").asOpt[List[Session]]).getOrElse(None)
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
}
