package common.repositories.event

import common.models.utils.Page
import common.models.event.Session
import common.models.event.SessionOld
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import common.repositories.user.UserActionRepository
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbSessionRepository extends Repository[Session] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.SESSIONS)

  private val crud = MongoDbCrudUtils(collection, Session.format, List("name", "description", "info.format", "info.category", "info.place", "info.speakers.name", "info.speakers.description", "info.speakers.company"), "uuid")

  def findAllOld(): Future[List[SessionOld]] = collection.find(Json.obj()).cursor[SessionOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Session]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Session]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[Session]] = crud.getByUuid(uuid)
  override def insert(elt: Session): Future[Option[Session]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Session): Future[Option[Session]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Session]] = {
    crud.delete(uuid).map { err =>
      UserActionRepository.deleteByItem(Session.className, uuid)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(uuids: List[String]): Future[List[Session]] = crud.findByUuids(uuids)
  def findByEvent(eventId: String, query: String = "", sort: String = ""): Future[List[Session]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId))
  def findPageByEvent(eventId: String, query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[Session]] = crud.findPage(query, page, pageSize, sort, Json.obj("eventId" -> eventId))
  def findByEventAttendee(eventId: String, attendeeId: String): Future[List[Session]] = crud.findAll(filter = Json.obj("eventId" -> eventId, "info.speakers" -> attendeeId))
  def findEventFormats(eventId: String): Future[List[String]] = crud.distinct("info.format", Json.obj("eventId" -> eventId)).map(_.filter(_ != ""))
  def findEventCategories(eventId: String): Future[List[String]] = crud.distinct("info.category", Json.obj("eventId" -> eventId)).map(_.filter(_ != ""))
  def findEventPlaces(eventId: String): Future[List[String]] = crud.distinct("info.place", Json.obj("eventId" -> eventId)).map(_.filter(_ != ""))
  def countForEvent(eventId: String): Future[Int] = crud.countFor("eventId", eventId)
  def countForEvents(eventIds: Seq[String]): Future[Map[String, Int]] = crud.countFor("eventId", eventIds)
  def deleteByEvent(eventId: String): Future[LastError] = crud.deleteBy("eventId", eventId)
  def bulkInsert(elts: List[Session]): Future[Int] = crud.bulkInsert(elts)
  def bulkUpdate(elts: List[(String, Session)]): Future[Int] = crud.bulkUpdate(elts)
  def bulkUpsert(elts: List[(String, Session)]): Future[Int] = crud.bulkUpsert(elts)
  def bulkDelete(uuids: List[String]): Future[LastError] = crud.bulkDelete(uuids)
  def drop(): Future[Boolean] = crud.drop()
}
object SessionRepository extends MongoDbSessionRepository
