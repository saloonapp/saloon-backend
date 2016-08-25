package common.repositories.conference

import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import conferences.models.{PersonId, Person}
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.commands.WriteResult

object PersonRepository {
  private val db = ReactiveMongoPlugin.db
  private lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.PERSONS)
  private val personFields = List("id", "name", "avatar", "twitter", "siteUrl", "email", "created", "createdBy")
  private def getFirst(fields: List[String]) = Json.parse("{\"_id\":\"$id\","+fields.map(f => "\""+f+"\":{\"$first\":\"$"+f+"\"}").mkString(",")+"}")

  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj("start" -> -1)): Future[List[Person]] = MongoDbCrudUtils.aggregate[List[Person]](collection, Json.obj(
    "aggregate" -> collection.name,
    "pipeline" -> Json.arr(
      Json.obj("$sort" -> Json.obj("created" -> -1)),
      Json.obj("$group" -> getFirst(personFields)),
      Json.obj("$match" -> filter),
      Json.obj("$sort" -> sort))))
  def findByIds(ids: List[PersonId], sort: JsObject = Json.obj("start" -> -1)): Future[List[Person]] = MongoDbCrudUtils.aggregate[List[Person]](collection, Json.obj(
    "aggregate" -> collection.name,
    "pipeline" -> Json.arr(
      Json.obj("$sort" -> Json.obj("created" -> -1)),
      Json.obj("$group" -> getFirst(personFields)),
      Json.obj("$match" -> Json.obj("$or" -> ids.distinct.map(id => Json.obj("id" -> Json.obj("$eq" -> id))))),
      Json.obj("$sort" -> sort))))
  def get(id: PersonId): Future[Option[Person]] = MongoDbCrudUtils.getFirst[Person](collection, Json.obj("id" -> id.unwrap), Json.obj("created" -> -1))
  def insert(elt: Person): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(created = new DateTime()))
  def update(id: PersonId, elt: Person): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt.copy(id = id, created = new DateTime()))
}
