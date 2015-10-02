package common.repositories.event

import common.models.utils.Page
import common.models.values.typed.ItemType
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.Exponent
import common.models.event.ExponentId
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

trait MongoDbExponentRepository extends Repository[Exponent, ExponentId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EXPONENTS)

  private val crud = MongoDbCrudUtils(collection, Exponent.format, List("name", "description", "info.place", "info.team.name", "info.team.description", "info.team.company"), "uuid")

  //def findAllOld(): Future[List[ExponentOld]] = collection.find(Json.obj()).cursor[ExponentOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Exponent]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Exponent]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(exponentId: ExponentId): Future[Option[Exponent]] = crud.getByUuid(exponentId.unwrap)
  override def insert(elt: Exponent): Future[Option[Exponent]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(exponentId: ExponentId, elt: Exponent): Future[Option[Exponent]] = crud.update(exponentId.unwrap, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(exponentId: ExponentId): Future[Option[Exponent]] = {
    crud.delete(exponentId.unwrap).map { err =>
      UserActionRepository.deleteByItem(ItemType.exponents, exponentId.unwrap)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(exponentIds: List[ExponentId]): Future[List[Exponent]] = crud.findByUuids(exponentIds.map(_.unwrap))
  def findByEvent(eventId: EventId, query: String = "", sort: String = ""): Future[List[Exponent]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId.unwrap))
  def findPageByEvent(eventId: EventId, query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[Exponent]] = crud.findPage(query, page, pageSize, sort, Json.obj("eventId" -> eventId.unwrap))
  def findByEventAttendee(eventId: EventId, attendeeId: AttendeeId): Future[List[Exponent]] = crud.findAll(filter = Json.obj("eventId" -> eventId.unwrap, "info.team" -> attendeeId.unwrap))
  def countForEvent(eventId: EventId): Future[Int] = crud.countFor("eventId", eventId.unwrap)
  def countForEvents(eventIds: Seq[EventId]): Future[Map[EventId, Int]] = crud.countFor("eventId", eventIds.map(_.unwrap)).map(_.map { case (key, value) => (EventId(key), value) })
  def addTeamMember(exponentId: ExponentId, attendeeId: AttendeeId): Future[UpdateWriteResult] = crud.update(Json.obj("uuid" -> exponentId.unwrap), Json.obj("$addToSet" -> Json.obj("info.team" -> attendeeId.unwrap)))
  def removeTeamMember(exponentId: ExponentId, attendeeId: AttendeeId): Future[UpdateWriteResult] = crud.update(Json.obj("uuid" -> exponentId.unwrap), Json.obj("$pull" -> Json.obj("info.team" -> attendeeId.unwrap)))
  def deleteByEvent(eventId: EventId): Future[WriteResult] = crud.deleteBy("eventId", eventId.unwrap)
  def bulkInsert(elts: List[Exponent]): Future[MultiBulkWriteResult] = crud.bulkInsert(elts)
  def bulkUpdate(elts: List[(ExponentId, Exponent)]): Future[Int] = crud.bulkUpdate(elts.map(p => (p._1.unwrap, p._2)))
  def bulkUpsert(elts: List[(ExponentId, Exponent)]): Future[Int] = crud.bulkUpsert(elts.map(p => (p._1.unwrap, p._2)))
  def bulkDelete(exponentIds: List[ExponentId]): Future[WriteResult] = crud.bulkDelete(exponentIds.map(_.unwrap))
  def drop(): Future[Boolean] = crud.drop()
}
object ExponentRepository extends MongoDbExponentRepository
