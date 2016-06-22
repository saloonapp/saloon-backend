package common.repositories.conference

import common.repositories.utils.MongoDbCrudUtils
import common.repositories.CollectionReferences
import conferences.models.{ConferenceId, Conference}
import org.joda.time.DateTime
import play.api.libs.json.{JsNull, Json, JsObject}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import play.api.Play.current

object ConferenceRepository {
  private val db = ReactiveMongoPlugin.db
  private lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.CONFERENCES)
  private val conferenceFields = List("id", "name", "description", "start", "end", "siteUrl", "videosUrl", "tags", "venue", "cfp", "tickets", "metrics", "social", "created", "createdBy")
  private def conferenceGroup(fields: List[String]) = Json.parse("{\"_id\":\"$id\","+fields.map(f => "\""+f+"\":{\"$first\":\"$"+f+"\"}").mkString(",")+"}")

  def findHistory(sort: JsObject = Json.obj("created" -> -1)): Future[List[Conference]] = MongoDbCrudUtils.find[Conference](collection, Json.obj(), sort)
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj("start" -> -1)): Future[List[Conference]] = MongoDbCrudUtils.aggregate[List[Conference]](collection, Json.obj(
      "aggregate" -> collection.name,
      "pipeline" -> Json.arr(
        Json.obj("$sort" -> Json.obj("created" -> -1)),
        Json.obj("$group" -> conferenceGroup(conferenceFields)),
        Json.obj("$match" -> filter),
        Json.obj("$sort" -> sort))))
  def insert(elt: Conference): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(created = new DateTime()))
  def update(id: ConferenceId, elt: Conference): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(id = id, created = new DateTime()))
  def get(id: ConferenceId, created: DateTime): Future[Option[Conference]] = MongoDbCrudUtils.get[Conference](collection, Json.obj("id" -> id.unwrap, "created" -> created))
  def get(id: ConferenceId): Future[Option[Conference]] = MongoDbCrudUtils.getFirst[Conference](collection, Json.obj("id" -> id.unwrap), Json.obj("created" -> -1))
  def getHistory(id: ConferenceId): Future[List[Conference]] = MongoDbCrudUtils.find[Conference](collection, Json.obj("id" -> id.unwrap), Json.obj("created" -> -1))
  def deleteVersion(id: ConferenceId, created: DateTime): Future[WriteResult] = MongoDbCrudUtils.delete(collection, Json.obj("id" -> id.unwrap, "created" -> created))
  def delete(id: ConferenceId): Future[WriteResult] = MongoDbCrudUtils.delete(collection, Json.obj("id" -> id.unwrap))
  def getTags(): Future[List[(String, Int)]] = MongoDbCrudUtils.aggregate2[List[(String, Int)]](
    collection,
    Json.obj(
      "aggregate" -> collection.name,
      "pipeline" -> Json.arr(
        Json.obj("$sort" -> Json.obj("created" -> -1)),
        Json.obj("$group" -> conferenceGroup(List("tags"))),
        Json.obj("$unwind" -> "$tags"),
        Json.obj("$group" -> Json.obj("_id" -> JsNull, "tags" -> Json.obj("$push" -> "$tags"))),
        Json.obj("$unwind" -> "$tags"),
        Json.obj("$group" -> Json.obj("_id" -> "$tags", "count" -> Json.obj("$sum" -> 1)))
      )
    ),
    json => (json \\ "_id").toList.map(_.as[String]).zip((json \\ "count").toList.map(_.as[Int])).sortBy(_._1)
  )
}
