package common.repositories.event

import common.models.utils.Page
import common.models.values.Source
import common.models.values.typed.EventStatus
import common.models.user.OrganizationId
import common.models.event.Event
import common.models.event.EventId
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import common.repositories.user.UserActionRepository
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONArray
import reactivemongo.core.commands.RawCommand
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats

trait MongoDbEventRepository extends Repository[Event, EventId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.EVENTS)

  private val crud = MongoDbCrudUtils(collection, Event.format, List("name", "description", "info.address.name", "info.address.street", "info.address.zipCode", "info.address.city", "info.social.twitter.hashtag", "info.social.twitter.account", "meta.categories"), "uuid")

  //def findAllOld(): Future[List[EventOld]] = collection.find(Json.obj()).cursor[EventOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Event]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Event]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(eventId: EventId): Future[Option[Event]] = crud.getByUuid(eventId.unwrap)
  override def insert(elt: Event): Future[Option[Event]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(eventId: EventId, elt: Event): Future[Option[Event]] = crud.update(eventId.unwrap, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(eventId: EventId): Future[Option[Event]] = {
    crud.delete(eventId.unwrap).map { err =>
      ExponentRepository.deleteByEvent(eventId)
      SessionRepository.deleteByEvent(eventId)
      UserActionRepository.deleteByEvent(eventId)
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

  def getBySources(sources: List[Source]): Future[Option[Event]] = crud.get(Json.obj("meta.source" -> sources.head)) // TODO build OR for all sources
  def findByUuids(eventIds: List[EventId]): Future[List[Event]] = crud.findByUuids(eventIds.map(_.unwrap))
  def findForOrganizations(organizationIds: List[OrganizationId]): Future[List[Event]] = crud.find(Json.obj("ownerId" -> Json.obj("$in" -> organizationIds.map(_.unwrap))))
  def setDraft(eventId: EventId): Future[LastError] = setStatus(eventId, EventStatus.draft)
  def setPublishing(eventId: EventId): Future[LastError] = setStatus(eventId, EventStatus.publishing)
  def setPublished(eventId: EventId): Future[LastError] = setStatus(eventId, EventStatus.published)
  private def setStatus(eventId: EventId, status: EventStatus): Future[LastError] = crud.update(Json.obj("uuid" -> eventId), Json.obj("$set" -> Json.obj("meta.status" -> status)))
  def bulkInsert(elts: List[Event]): Future[Int] = crud.bulkInsert(elts)
  def drop(): Future[Boolean] = crud.drop()
}
object EventRepository extends MongoDbEventRepository
