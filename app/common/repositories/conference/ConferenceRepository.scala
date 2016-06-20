package common.repositories.conference

import common.repositories.utils.MongoDbCrudUtils
import common.repositories.CollectionReferences
import conferences.models.{ConferenceId, Conference}
import org.joda.time.DateTime
import play.api.libs.json.{Json, JsObject}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import play.api.Play.current

object ConferenceRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.CONFERENCES)
  val conferenceFields = List("id", "name", "description", "start", "end", "siteUrl", "videosUrl", "tags", "venue", "cfp", "tickets", "metrics", "social", "created")
  val conferenceGroup = Json.parse("{\"_id\":\"$id\","+conferenceFields.map(f => "\""+f+"\":{\"$first\":\"$"+f+"\"}").mkString(",")+"}")

  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj("start" -> 1)): Future[List[Conference]] = MongoDbCrudUtils.aggregate[List[Conference]](collection, Json.obj(
      "aggregate" -> collection.name,
      "pipeline" -> Json.arr(
        Json.obj("$sort" -> Json.obj("created" -> -1)),
        Json.obj("$group" -> conferenceGroup),
        Json.obj("$match" -> filter),
        Json.obj("$sort" -> sort))))
  def insert(elt: Conference): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(created = new DateTime()))
  def update(id: ConferenceId, elt: Conference): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(id = id, created = new DateTime()))
  def get(id: ConferenceId, created: DateTime): Future[Option[Conference]] = MongoDbCrudUtils.get[Conference](collection, Json.obj("id" -> id.unwrap, "created" -> created))
  def get(id: ConferenceId): Future[Option[Conference]] = MongoDbCrudUtils.getFirst[Conference](collection, Json.obj("id" -> id.unwrap), Json.obj("created" -> -1))
  def getHistory(id: ConferenceId): Future[List[Conference]] = MongoDbCrudUtils.find[Conference](collection, Json.obj("id" -> id.unwrap), Json.obj("created" -> -1))
  def deleteVersion(id: ConferenceId, created: DateTime): Future[WriteResult] = MongoDbCrudUtils.delete(collection, Json.obj("id" -> id.unwrap, "created" -> created))
  def delete(id: ConferenceId): Future[WriteResult] = MongoDbCrudUtils.delete(collection, Json.obj("id" -> id.unwrap))
}
