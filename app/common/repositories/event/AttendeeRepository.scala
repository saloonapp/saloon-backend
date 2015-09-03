package common.repositories.event

import common.models.utils.Page
import common.models.event.EventId
import common.models.event.Attendee
import common.models.event.AttendeeId
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

trait MongoDbAttendeeRepository extends Repository[Attendee, AttendeeId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.ATTENDEES)

  private val crud = MongoDbCrudUtils(collection, Attendee.format, List("name", "description", "info.website", "info.company", "info.job", "info.role", "social.blogUrl", "social.facebookUrl", "social.twitterUrl", "social.linkedinUrl", "social.githubUrl"), "uuid")

  //def findAllOld(): Future[List[AttendeeOld]] = collection.find(Json.obj()).cursor[AttendeeOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Attendee]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Attendee]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(attendeeId: AttendeeId): Future[Option[Attendee]] = crud.getByUuid(attendeeId.unwrap)
  override def insert(elt: Attendee): Future[Option[Attendee]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(attendeeId: AttendeeId, elt: Attendee): Future[Option[Attendee]] = crud.update(attendeeId.unwrap, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(attendeeId: AttendeeId): Future[Option[Attendee]] = {
    crud.delete(attendeeId.unwrap).map { err =>
      UserActionRepository.deleteByItem(Attendee.className, attendeeId.unwrap)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(attendeeIds: List[AttendeeId]): Future[List[Attendee]] = crud.findByUuids(attendeeIds.map(_.unwrap))
  def findByEvent(eventId: EventId, query: String = "", sort: String = ""): Future[List[Attendee]] = crud.findAll(query, sort, Json.obj("eventId" -> eventId.unwrap))
  def findPageByEvent(eventId: EventId, query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[Attendee]] = crud.findPage(query, page, pageSize, sort, Json.obj("eventId" -> eventId.unwrap))
  def findEventRoles(eventId: EventId): Future[List[String]] = crud.distinct("info.role", Json.obj("eventId" -> eventId.unwrap)).map(_.filter(_ != ""))
  def countForEvent(eventId: EventId): Future[Int] = crud.countFor("eventId", eventId.unwrap)
  def countForEvents(eventIds: Seq[EventId]): Future[Map[EventId, Int]] = crud.countFor("eventId", eventIds.map(_.unwrap)).map(_.map { case (key, value) => (EventId(key), value) })
  def deleteByEvent(eventId: EventId): Future[LastError] = crud.deleteBy("eventId", eventId.unwrap)
  def bulkInsert(elts: List[Attendee]): Future[Int] = crud.bulkInsert(elts)
  def bulkUpdate(elts: List[(AttendeeId, Attendee)]): Future[Int] = crud.bulkUpdate(elts.map(p => (p._1.unwrap, p._2)))
  def bulkUpsert(elts: List[(AttendeeId, Attendee)]): Future[Int] = crud.bulkUpsert(elts.map(p => (p._1.unwrap, p._2)))
  def bulkDelete(attendeeIds: List[AttendeeId]): Future[LastError] = crud.bulkDelete(attendeeIds.map(_.unwrap))
  def drop(): Future[Boolean] = crud.drop()
}
object AttendeeRepository extends MongoDbAttendeeRepository
