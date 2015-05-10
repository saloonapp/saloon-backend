package infrastructure.repository

import infrastructure.repository.common.Repository
import infrastructure.repository.common.MongoDbCrudUtils
import models.common.Page
import models.Session
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbSessionRepository extends Repository[Session] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.SESSIONS)

  private val crud = MongoDbCrudUtils(collection, Session.format, List("title", "summary", "place.ref", "place.name", "format", "category", "tags"), "uuid")

  override def findAll(query: String = "", sort: String = ""): Future[List[Session]] = crud.findAll(query, sort)
  override def findPage(query: String = "", page: Int = 1, sort: String = ""): Future[Page[Session]] = crud.findPage(query, page, sort)
  override def getByUuid(uuid: String): Future[Option[Session]] = crud.getByUuid(uuid)
  override def insert(elt: Session): Future[Option[Session]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Session): Future[Option[Session]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Session]] = crud.delete(uuid).map(err => None) // TODO : return deleted elt !

  def findByEvent(eventId: String, query: String = "", sort: String = ""): Future[List[Session]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId))
  def findPageByEvent(eventId: String, query: String = "", page: Int = 1, sort: String = ""): Future[Page[Session]] = crud.findPage(query, page, sort, filter = Json.obj("eventId" -> eventId))
  def countForEvent(eventId: String): Future[Int] = crud.countFor("eventId", eventId)
  def countForEvents(eventIds: Seq[String]): Future[Map[String, Int]] = crud.countFor("eventId", eventIds)
  def deleteByEvent(eventId: String): Future[LastError] = crud.deleteBy("eventId", eventId)
  def bulkInsert(elts: List[Session]): Future[Int] = crud.bulkInsert(elts)
  def drop(): Future[Boolean] = crud.drop()
}
object SessionRepository extends MongoDbSessionRepository
