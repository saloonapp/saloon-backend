package infrastructure.repository

import common.models.Page
import common.infrastructure.repository.Repository
import common.infrastructure.repository.MongoDbCrudUtils
import models.event.Event
import models.event.EventOld
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbEventRepository extends Repository[Event] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EVENTS)

  private val crud = MongoDbCrudUtils(collection, Event.format, List("name", "description", "address.name", "address.street", "address.zipCode", "address.city", "twitterHashtag", "twitterAccount", "tags"), "uuid")

  def findAllOld(): Future[List[EventOld]] = collection.find(Json.obj()).cursor[EventOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Event]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Event]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[Event]] = crud.getByUuid(uuid)
  override def insert(elt: Event): Future[Option[Event]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Event): Future[Option[Event]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Event]] = {
    crud.delete(uuid).map { err =>
      ExponentRepository.deleteByEvent(uuid)
      SessionRepository.deleteByEvent(uuid)
      UserActionRepository.deleteByEvent(uuid)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(uuids: List[String]): Future[List[Event]] = crud.findByUuids(uuids)
  def bulkInsert(elts: List[Event]): Future[Int] = crud.bulkInsert(elts)
  def drop(): Future[Boolean] = crud.drop()
}
object EventRepository extends MongoDbEventRepository
