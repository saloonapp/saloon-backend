package infrastructure.repository

import infrastructure.repository.common.Repository
import infrastructure.repository.common.MongoDbCrudUtils
import models.common.Page
import models.Exponent
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbExponentRepository extends Repository[Exponent] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EXPONENTS)

  private val crud = MongoDbCrudUtils(collection, Exponent.format, List("name", "description", "place.ref", "place.name", "company", "tags"), "uuid")

  override def findAll(query: String = "", sort: String = ""): Future[List[Exponent]] = crud.findAll(query, sort)
  override def findPage(query: String = "", page: Int = 1, sort: String = ""): Future[Page[Exponent]] = crud.findPage(query, page, sort)
  override def getByUuid(uuid: String): Future[Option[Exponent]] = crud.getByUuid(uuid)
  override def insert(elt: Exponent): Future[Option[Exponent]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Exponent): Future[Option[Exponent]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Exponent]] = crud.delete(uuid).map(err => None) // TODO : return deleted elt !

  def findByEvent(eventId: String, query: String = "", sort: String = ""): Future[List[Exponent]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId))
  def findPageByEvent(eventId: String, query: String = "", page: Int = 1, sort: String = ""): Future[Page[Exponent]] = crud.findPage(query, page, sort, filter = Json.obj("eventId" -> eventId))
  def countForEvent(eventId: String): Future[Int] = crud.countFor("eventId", eventId)
  def countForEvents(eventIds: Seq[String]): Future[Map[String, Int]] = crud.countFor("eventId", eventIds)
}
object ExponentRepository extends MongoDbExponentRepository
