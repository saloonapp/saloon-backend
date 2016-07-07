package common.repositories.conference

import common.{Defaults, Utils}
import common.repositories.utils.MongoDbCrudUtils
import common.repositories.CollectionReferences
import conferences.models.{ConferenceId, Conference}
import org.joda.time.DateTime
import play.api.libs.json.{JsNull, Json, JsObject}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

import scala.util.Try

object ConferenceRepository {
  private val db = ReactiveMongoPlugin.db
  private lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.CONFERENCES)
  private val conferenceFields = List("id", "name", "logo", "description", "start", "end", "siteUrl", "videosUrl", "tags", "location", "cfp", "tickets", "social", "created", "createdBy")
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
  def findInPast(time: DateTime, filter: JsObject = Json.obj(), sort: JsObject = Json.obj("start" -> -1)): Future[List[Conference]] = MongoDbCrudUtils.aggregate[List[Conference]](collection, Json.obj(
    "aggregate" -> collection.name,
    "pipeline" -> Json.arr(
      Json.obj("$match" -> Json.obj("created" -> Json.obj("$lt" -> time))),
      Json.obj("$sort" -> Json.obj("created" -> -1)),
      Json.obj("$group" -> conferenceGroup(conferenceFields)),
      Json.obj("$match" -> filter),
      Json.obj("$sort" -> sort))))
  def importData(elts: List[Conference]): Future[MultiBulkWriteResult] = {
    if(Utils.isProd()){ throw new IllegalStateException("You can't import data in prod !!!") }
    MongoDbCrudUtils.drop(collection).flatMap { dropSuccess =>
      MongoDbCrudUtils.bulkInsert(elts, collection)
    }
  }

  def buildSearchFilter(q: Option[String], period: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]): JsObject = {
    def reduce(l: List[Option[JsObject]]): Option[JsObject] = l.flatten.headOption.map(_ => l.flatten.reduceLeft(_ ++ _))
    def parseDate(d: String): Option[DateTime] = Try(DateTime.parse(d, Defaults.dateFormatter)).toOption
    def buildTextFilter(q: Option[String]): Option[JsObject] =
      q.map(_.trim).filter(_.length > 0).map { query =>
        Json.obj("$or" -> List(
          "name", "description", "siteUrl", "videosUrl", "tags",
          "location.name", "location.street", "location.postalCode", "location.locality", "location.country",
          "cfp.siteUrl", "tickets.siteUrl", "social.twitter.account", "social.twitter.hashtag"
        ).map(field => Json.obj(field -> Json.obj("$regex" -> query, "$options" -> "i"))))
      }
    def buildDateFilter(before: Option[String], after: Option[String]): Option[JsObject] =
      reduce(List(
        before.flatMap(parseDate).map(d => Json.obj("$lte" -> d)),
        after.flatMap(parseDate).map(d => Json.obj("$gte" -> d))
      )).map(f => Json.obj("end" -> f))
    def buildTagFilter(tagsOpt: Option[String]): Option[JsObject] =
      tagsOpt
        .map(_.split(",").map(_.trim).filter(_.length > 0))
        .filter(_.length > 0)
        .map(tags => Json.obj("tags" -> Json.obj("$in" ->tags)))
    def buildCfpFilter(cfp: Option[String]): Option[JsObject] = cfp match {
      case Some("on") => Some(Json.obj("cfp.end" -> Json.obj("$gte" -> new DateTime())))
      case _ => None
    }
    def buildTicketsFilter(tickets: Option[String]): Option[JsObject] = tickets match {
      case Some("on") => Some(Json.obj("end" -> Json.obj("$lte" -> new DateTime()), "tickets.siteUrl" -> Json.obj("$exists" -> true)))
      case _ => None
    }
    def buildVideosFilter(videos: Option[String]): Option[JsObject] = videos match {
      case Some("on") => Some(Json.obj("videosUrl" -> Json.obj("$exists" -> true, "$ne" -> JsNull)))
      case _ => None
    }
    val (pAfter, pBefore) = period.map(_.split(" - ") match {
      case Array(a, b) => (Some(a), Some(b))
      case _ => (None, None)
    }).getOrElse((None, None))
    val filters = List(
      buildTextFilter(q),
      buildDateFilter(before.orElse(pBefore), after.orElse(pAfter)),
      buildTagFilter(tags),
      buildCfpFilter(cfp),
      buildTicketsFilter(tickets),
      buildVideosFilter(videos)
    ).flatten
    if(filters.length == 0) Json.obj()
    else if(filters.length == 1) filters.head
    else Json.obj("$and" -> filters)
  }
}
