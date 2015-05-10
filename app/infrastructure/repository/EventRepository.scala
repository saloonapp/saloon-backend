package infrastructure.repository

import infrastructure.repository.common.Repository
import infrastructure.repository.common.MongoDbCrudUtils
import models.common.Page
import models.Event
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbEventRepository extends Repository[Event] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EVENTS)

  private val crud = MongoDbCrudUtils(collection, Event.format, List("name", "description", "address", "twitterHashtag"), "uuid")

  override def findAll(query: String = "", sort: String = ""): Future[List[Event]] = crud.findAll(query, sort)
  override def findPage(query: String = "", page: Int = 1, sort: String = ""): Future[Page[Event]] = crud.findPage(query, page, sort)
  override def getByUuid(uuid: String): Future[Option[Event]] = crud.getByUuid(uuid)
  override def insert(elt: Event): Future[Option[Event]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Event): Future[Option[Event]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Event]] = crud.delete(uuid).map(err => None) // TODO : return deleted elt !

  def bulkInsert(elts: List[Event]): Future[Int] = crud.bulkInsert(elts)
  def drop(): Future[Boolean] = crud.drop()
}
object EventRepository extends MongoDbEventRepository
