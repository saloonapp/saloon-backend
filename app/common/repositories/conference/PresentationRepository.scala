package common.repositories.conference

import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import conferences.models.{PersonId, PresentationId, Presentation, ConferenceId}
import org.joda.time.DateTime
import play.api.libs.json.{Reads, JsNull, JsObject, Json}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.commands.WriteResult

object PresentationRepository {
  private val db = ReactiveMongoPlugin.db
  private lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.PRESENTATIONS)
  private val presentationFields = List("conferenceId", "id", "title", "description", "slidesUrl", "videoUrl", "speakers", "start", "end", "room", "tags", "created", "createdBy")
  private def getFirst(fields: List[String]) = Json.parse("{\"_id\":\"$id\","+fields.map(f => "\""+f+"\":{\"$first\":\"$"+f+"\"}").mkString(",")+"}")

  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj("title" -> 1)): Future[List[Presentation]] = MongoDbCrudUtils.aggregate[List[Presentation]](collection, Json.obj(
    "aggregate" -> collection.name,
    "pipeline" -> Json.arr(
      Json.obj("$sort" -> Json.obj("created" -> -1)),
      Json.obj("$group" -> getFirst(presentationFields)),
      Json.obj("$match" -> filter),
      Json.obj("$sort" -> sort))))
  def findForConference(cId: ConferenceId, filter: JsObject = Json.obj(), sort: JsObject = Json.obj("title" -> 1)): Future[List[Presentation]] = find(Json.obj("conferenceId" -> cId.unwrap), sort)
  def findForPerson(pId: PersonId, sort: JsObject = Json.obj("title" -> 1)): Future[List[Presentation]] = find(Json.obj("speakers" -> pId.unwrap), sort)
  def insert(elt: Presentation): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(created = new DateTime()))
  def update(cId: ConferenceId, pId: PresentationId, elt: Presentation): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(conferenceId = cId, id = pId, created = new DateTime()))
  def get(cId: ConferenceId, pId: PresentationId, created: DateTime): Future[Option[Presentation]] = MongoDbCrudUtils.get[Presentation](collection, Json.obj("conferenceId" -> cId.unwrap, "id" -> pId.unwrap, "created" -> created))
  def get(cId: ConferenceId, pId: PresentationId): Future[Option[Presentation]] = MongoDbCrudUtils.getFirst[Presentation](collection, Json.obj("conferenceId" -> cId.unwrap, "id" -> pId.unwrap), Json.obj("created" -> -1))
  def getTags(): Future[List[(String, Int)]] = MongoDbCrudUtils.aggregate2[List[(String, Int)]](
    collection,
    Json.obj(
      "aggregate" -> collection.name,
      "pipeline" -> Json.arr(
        Json.obj("$sort" -> Json.obj("created" -> -1)),
        Json.obj("$group" -> getFirst(List("tags"))),
        Json.obj("$unwind" -> "$tags"),
        Json.obj("$group" -> Json.obj("_id" -> JsNull, "tags" -> Json.obj("$push" -> "$tags"))),
        Json.obj("$unwind" -> "$tags"),
        Json.obj("$group" -> Json.obj("_id" -> "$tags", "count" -> Json.obj("$sum" -> 1)))
      )
    ),
    json => (json \\ "_id").toList.map(_.as[String]).zip((json \\ "count").toList.map(_.as[Int])).sortBy(-_._2)
  )
  def getRooms(cId: ConferenceId): Future[List[(String, Int)]] = MongoDbCrudUtils.aggregate2[List[(String, Int)]](
    collection,
    Json.obj(
      "aggregate" -> collection.name,
      "pipeline" -> Json.arr(
        Json.obj("$match" -> Json.obj("conferenceId" -> cId.unwrap)),
        Json.obj("$sort" -> Json.obj("created" -> -1)),
        Json.obj("$group" -> getFirst(List("room"))),
        Json.obj("$match" -> Json.obj("room" -> Json.obj("$ne" -> JsNull))),
        Json.obj("$group" -> Json.obj("_id" -> "$room", "count" -> Json.obj("$sum" -> 1)))
      )
    ),
    json => (json \\ "_id").toList.map(_.as[String]).zip((json \\ "count").toList.map(_.as[Int])).sortBy(-_._2)
  )
}
