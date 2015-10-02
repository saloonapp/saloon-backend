package common.repositories.event

import common.models.utils.Page
import common.models.values.typed.ItemType
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.Session
import common.models.event.SessionId
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import common.repositories.user.UserActionRepository
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.commands.MultiBulkWriteResult
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbSessionRepository extends Repository[Session, SessionId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.SESSIONS)

  private val crud = MongoDbCrudUtils(collection, Session.format, List("name", "description", "info.format", "info.category", "info.place", "info.speakers.name", "info.speakers.description", "info.speakers.company"), "uuid")

  //def findAllOld(): Future[List[SessionOld]] = collection.find(Json.obj()).cursor[SessionOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Session]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Session]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(sessionId: SessionId): Future[Option[Session]] = crud.getByUuid(sessionId.unwrap)
  override def insert(elt: Session): Future[Option[Session]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(sessionId: SessionId, elt: Session): Future[Option[Session]] = crud.update(sessionId.unwrap, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(sessionId: SessionId): Future[Option[Session]] = {
    crud.delete(sessionId.unwrap).map { err =>
      UserActionRepository.deleteByItem(ItemType.sessions, sessionId.unwrap)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(sessionIds: List[SessionId]): Future[List[Session]] = crud.findByUuids(sessionIds.map(_.unwrap))
  def findByEvent(eventId: EventId, query: String = "", sort: String = ""): Future[List[Session]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId.unwrap))
  def findPageByEvent(eventId: EventId, query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[Session]] = crud.findPage(query, page, pageSize, sort, Json.obj("eventId" -> eventId.unwrap))
  def findByEventAttendee(eventId: EventId, attendeeId: AttendeeId): Future[List[Session]] = crud.findAll(filter = Json.obj("eventId" -> eventId.unwrap, "info.speakers" -> attendeeId.unwrap))
  def findEventFormats(eventId: EventId): Future[List[String]] = crud.distinct("info.format", Json.obj("eventId" -> eventId.unwrap)).map(_.filter(_ != ""))
  def findEventCategories(eventId: EventId): Future[List[String]] = crud.distinct("info.category", Json.obj("eventId" -> eventId.unwrap)).map(_.filter(_ != ""))
  def findEventPlaces(eventId: EventId): Future[List[String]] = crud.distinct("info.place", Json.obj("eventId" -> eventId.unwrap)).map(_.filter(_ != ""))
  def countForEvent(eventId: EventId): Future[Int] = crud.countFor("eventId", eventId.unwrap)
  def countForEvents(eventIds: Seq[EventId]): Future[Map[EventId, Int]] = crud.countFor("eventId", eventIds.map(_.unwrap)).map(_.map { case (key, value) => (EventId(key), value) })
  def addSpeaker(sessionId: SessionId, attendeeId: AttendeeId): Future[UpdateWriteResult] = crud.update(Json.obj("uuid" -> sessionId.unwrap), Json.obj("$addToSet" -> Json.obj("info.speakers" -> attendeeId.unwrap)))
  def removeSpeaker(sessionId: SessionId, attendeeId: AttendeeId): Future[UpdateWriteResult] = crud.update(Json.obj("uuid" -> sessionId.unwrap), Json.obj("$pull" -> Json.obj("info.speakers" -> attendeeId.unwrap)))
  def deleteByEvent(eventId: EventId): Future[WriteResult] = crud.deleteBy("eventId", eventId.unwrap)
  def bulkInsert(elts: List[Session]): Future[MultiBulkWriteResult] = crud.bulkInsert(elts)
  def bulkUpdate(elts: List[(SessionId, Session)]): Future[Int] = crud.bulkUpdate(elts.map(p => (p._1.unwrap, p._2)))
  def bulkUpsert(elts: List[(SessionId, Session)]): Future[Int] = crud.bulkUpsert(elts.map(p => (p._1.unwrap, p._2)))
  def bulkDelete(sessionIds: List[SessionId]): Future[WriteResult] = crud.bulkDelete(sessionIds.map(_.unwrap))
  def drop(): Future[Boolean] = crud.drop()
}
object SessionRepository extends MongoDbSessionRepository
