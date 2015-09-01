package common.repositories.event

import common.models.utils.Page
import common.models.event.Exponent
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

trait MongoDbExponentRepository extends Repository[Exponent] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EXPONENTS)

  private val crud = MongoDbCrudUtils(collection, Exponent.format, List("name", "description", "info.place", "info.team.name", "info.team.description", "info.team.company"), "uuid")

  //def findAllOld(): Future[List[ExponentOld]] = collection.find(Json.obj()).cursor[ExponentOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Exponent]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Exponent]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[Exponent]] = crud.getByUuid(uuid)
  override def insert(elt: Exponent): Future[Option[Exponent]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Exponent): Future[Option[Exponent]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Exponent]] = {
    crud.delete(uuid).map { err =>
      UserActionRepository.deleteByItem(Exponent.className, uuid)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(uuids: List[String]): Future[List[Exponent]] = crud.findByUuids(uuids)
  def findByEvent(eventId: String, query: String = "", sort: String = ""): Future[List[Exponent]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId))
  def findPageByEvent(eventId: String, query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[Exponent]] = crud.findPage(query, page, pageSize, sort, Json.obj("eventId" -> eventId))
  def findByEventAttendee(eventId: String, attendeeId: String): Future[List[Exponent]] = crud.findAll(filter = Json.obj("eventId" -> eventId, "info.team" -> attendeeId))
  def countForEvent(eventId: String): Future[Int] = crud.countFor("eventId", eventId)
  def countForEvents(eventIds: Seq[String]): Future[Map[String, Int]] = crud.countFor("eventId", eventIds)
  def addTeamMember(uuid: String, attendeeId: String): Future[LastError] = crud.update(Json.obj("uuid" -> uuid), Json.obj("$addToSet" -> Json.obj("info.team" -> attendeeId)))
  def removeTeamMember(uuid: String, attendeeId: String): Future[LastError] = crud.update(Json.obj("uuid" -> uuid), Json.obj("$pull" -> Json.obj("info.team" -> attendeeId)))
  def removeFromAllTeams(attendeeId: String): Future[LastError] = crud.update(Json.obj(), Json.obj("$pull" -> Json.obj("info.team" -> attendeeId)), multi = true)
  def deleteByEvent(eventId: String): Future[LastError] = crud.deleteBy("eventId", eventId)
  def bulkInsert(elts: List[Exponent]): Future[Int] = crud.bulkInsert(elts)
  def bulkUpdate(elts: List[(String, Exponent)]): Future[Int] = crud.bulkUpdate(elts)
  def bulkUpsert(elts: List[(String, Exponent)]): Future[Int] = crud.bulkUpsert(elts)
  def bulkDelete(uuids: List[String]): Future[LastError] = crud.bulkDelete(uuids)
  def drop(): Future[Boolean] = crud.drop()
}
object ExponentRepository extends MongoDbExponentRepository
