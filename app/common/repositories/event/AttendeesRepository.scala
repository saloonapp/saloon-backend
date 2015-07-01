package common.repositories.event

import common.models.utils.Page
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import common.repositories.user.UserActionRepository
import common.models.event.Attendee
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbAttendeeRepository extends Repository[Attendee] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.ATTENDEES)

  private val crud = MongoDbCrudUtils(collection, Attendee.format, List("name", "description", "info.website", "info.company", "info.job", "info.role", "social.blogUrl", "social.facebookUrl", "social.twitterUrl", "social.linkedinUrl", "social.githubUrl"), "uuid")

  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Attendee]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Attendee]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[Attendee]] = crud.getByUuid(uuid)
  override def insert(elt: Attendee): Future[Option[Attendee]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Attendee): Future[Option[Attendee]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Attendee]] = {
    crud.delete(uuid).map { err =>
      UserActionRepository.deleteByItem(Attendee.className, uuid)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(uuids: List[String]): Future[List[Attendee]] = crud.findByUuids(uuids)
  def findByEvent(eventId: String, query: String = "", sort: String = ""): Future[List[Attendee]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId))
  def findPageByEvent(eventId: String, query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[Attendee]] = crud.findPage(query, page, pageSize, sort, Json.obj("eventId" -> eventId))
  def countForEvent(eventId: String): Future[Int] = crud.countFor("eventId", eventId)
  def countForEvents(eventIds: Seq[String]): Future[Map[String, Int]] = crud.countFor("eventId", eventIds)
  def deleteByEvent(eventId: String): Future[LastError] = crud.deleteBy("eventId", eventId)
  def bulkInsert(elts: List[Attendee]): Future[Int] = crud.bulkInsert(elts)
  def bulkUpdate(elts: List[(String, Attendee)]): Future[Int] = crud.bulkUpdate(elts)
  def bulkUpsert(elts: List[(String, Attendee)]): Future[Int] = crud.bulkUpsert(elts)
  def bulkDelete(uuids: List[String]): Future[LastError] = crud.bulkDelete(uuids)
  def drop(): Future[Boolean] = crud.drop()
}
object AttendeeRepository extends MongoDbAttendeeRepository
