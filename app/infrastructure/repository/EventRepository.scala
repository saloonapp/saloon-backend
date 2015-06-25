package infrastructure.repository

import common.models.Page
import common.infrastructure.repository.Repository
import common.infrastructure.repository.MongoDbCrudUtils
import models.event.Event
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONArray
import reactivemongo.core.commands.RawCommand
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats

trait MongoDbEventRepository extends Repository[Event] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EVENTS)

  private val crud = MongoDbCrudUtils(collection, Event.format, List("name", "description", "info.address.name", "info.address.street", "info.address.zipCode", "info.address.city", "info.social.twitter.hashtag", "info.social.twitter.account", "meta.categories"), "uuid")

  //def findAllOld(): Future[List[EventOld]] = collection.find(Json.obj()).cursor[EventOld].collect[List]()
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

  def getCategories(): Future[List[String]] = {
    val commandDoc = BSONDocument(
      "aggregate" -> collection.name,
      "pipeline" -> BSONArray(
        BSONDocument("$unwind" -> "$meta.categories"),
        BSONDocument("$group" -> BSONFormats.BSONDocumentFormat.reads(Json.obj("_id" -> JsNull, "categories" -> Json.obj("$addToSet" -> "$meta.categories"))).get)))

    collection.db.command(RawCommand(commandDoc)).map { result =>
      import play.modules.reactivemongo.json.BSONFormats._
      // {"result":[{"_id":null,"categories":["drupal","tech","emploi"]}],"ok":1.0}
      (Json.toJson(result) \ "result").as[JsArray].value.headOption.map { res => (res \ "categories").as[List[String]] }.getOrElse(List())
    }
  }

  def findByUuids(uuids: List[String]): Future[List[Event]] = crud.findByUuids(uuids)
  def bulkInsert(elts: List[Event]): Future[Int] = crud.bulkInsert(elts)
  def drop(): Future[Boolean] = crud.drop()
}
object EventRepository extends MongoDbEventRepository
