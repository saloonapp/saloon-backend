package common.repositories.utils

import common.Utils
import common.models.utils.Page
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats
import play.modules.reactivemongo.json.JsObjectDocumentWriter
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.core.commands.Count
import reactivemongo.core.commands.Drop
import reactivemongo.core.commands.RawCommand
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONArray

/*
 * Convention :
 *  - methods get* return one result (Option[T])
 *  - methods find* return a list of results (List[T])
 */
case class MongoDbCrudUtils[T](
  collection: JSONCollection,
  format: Format[T],
  filterFields: List[String],
  fieldUuid: String) {
  implicit val r: Reads[T] = format
  implicit val w: Writes[T] = format
  implicit val ow = new OWrites[T] { override def writes(value: T): JsObject = w.writes(value).as[JsObject] }
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[List[T]] = MongoDbCrudUtils.find(collection, filter, sort)
  def get(filter: JsObject = Json.obj()): Future[Option[T]] = MongoDbCrudUtils.get(collection, filter)
  def insert(elt: T): Future[WriteResult] = MongoDbCrudUtils.insert(collection, elt)
  def update(filter: JsObject, elt: T): Future[UpdateWriteResult] = MongoDbCrudUtils.update(collection, filter, elt)
  def update(filter: JsObject, elt: JsObject, multi: Boolean = false): Future[UpdateWriteResult] = MongoDbCrudUtils.update(collection, filter, elt, false, multi)
  def upsert(filter: JsObject, elt: T): Future[UpdateWriteResult] = MongoDbCrudUtils.upsert(collection, filter, elt)
  def delete(filter: JsObject = Json.obj()): Future[WriteResult] = MongoDbCrudUtils.delete(collection, filter)
  def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[T]] = MongoDbCrudUtils.findAll(collection, filter, query, filterFields, sort)
  def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[T]] = MongoDbCrudUtils.findPage(collection, filter, query, filterFields, page, pageSize, sort)
  def findBy(fieldName: String, fieldValue: String): Future[List[T]] = MongoDbCrudUtils.findBy(fieldValue, collection, fieldName)
  def countFor(fieldName: String, fieldValue: String): Future[Int] = MongoDbCrudUtils.countFor(fieldValue, collection, fieldName)
  def getByUuid(uuid: String): Future[Option[T]] = MongoDbCrudUtils.getBy(uuid, collection, fieldUuid)
  def findByUuids(uuids: Seq[String]): Future[List[T]] = MongoDbCrudUtils.findByList(uuids, collection, fieldUuid)
  def getBy(fieldName: String, fieldValue: String): Future[Option[T]] = MongoDbCrudUtils.getBy(fieldValue, collection, fieldName)
  def findBy(fieldName: String, fieldValues: Seq[String]): Future[List[T]] = MongoDbCrudUtils.findByList(fieldValues, collection, fieldName)
  def countFor(fieldName: String, fieldValues: Seq[String]): Future[Map[String, Int]] = MongoDbCrudUtils.countForList(fieldValues, collection, fieldName)
  def count(filter: JsObject, group: String): Future[Map[String, Int]] = MongoDbCrudUtils.count(filter, group, collection)
  def distinct(field: String, filter: JsObject = Json.obj()): Future[List[String]] = MongoDbCrudUtils.distinct(field, filter, collection)
  def update(uuid: String, elt: T): Future[UpdateWriteResult] = MongoDbCrudUtils.update(uuid, elt, collection, fieldUuid)
  def delete(uuid: String): Future[WriteResult] = MongoDbCrudUtils.deleteBy(uuid, collection, fieldUuid)
  def deleteBy(fieldName: String, fieldValue: String): Future[WriteResult] = MongoDbCrudUtils.deleteBy(fieldValue, collection, fieldName)
  def bulkInsert(elts: List[T]): Future[MultiBulkWriteResult] = MongoDbCrudUtils.bulkInsert(elts, collection)
  def bulkUpdate(elts: List[(String, T)]): Future[Int] = MongoDbCrudUtils.bulkUpdate(elts, collection)
  def bulkUpsert(elts: List[(String, T)]): Future[Int] = MongoDbCrudUtils.bulkUpsert(elts, collection)
  def bulkDelete(uuids: List[String]): Future[WriteResult] = MongoDbCrudUtils.bulkDelete(uuids, collection)
  def drop(): Future[Boolean] = MongoDbCrudUtils.drop(collection)
}
object MongoDbCrudUtils {
  implicit val s = play.libs.Akka.system.scheduler

  def find[T](collection: JSONCollection, filter: JsObject = Json.obj(), sort: JsObject = Json.obj())(implicit r: Reads[T]): Future[List[T]] = Utils.retryOnce {
    collection.find(filter).sort(sort).cursor[T].collect[List]()
  }

  def get[T](collection: JSONCollection, filter: JsObject = Json.obj())(implicit r: Reads[T]): Future[Option[T]] = Utils.retryOnce {
    collection.find(filter).one[T]
  }

  def getFirst[T](collection: JSONCollection, filter: JsObject = Json.obj(), sort: JsObject = Json.obj())(implicit r: Reads[T]): Future[Option[T]] = Utils.retryOnce {
    collection.find(filter).sort(sort).cursor[T].headOption
  }

  def insert[T](collection: JSONCollection, elt: T)(implicit w: Writes[T]): Future[WriteResult] = Utils.retryOnce {
    collection.save(elt)
  }

  def update[T](collection: JSONCollection, filter: JsObject, elt: T)(implicit w: Writes[T], ow: OWrites[T]): Future[UpdateWriteResult] = Utils.retryOnce {
    collection.update(filter, elt)
  }

  def update[T](collection: JSONCollection, query: JsObject, update: JsObject, upsert: Boolean, multi: Boolean): Future[UpdateWriteResult] = Utils.retryOnce {
    collection.update(query, update)
  }

  def upsert[T](collection: JSONCollection, filter: JsObject, elt: T)(implicit w: Writes[T], ow: OWrites[T]): Future[UpdateWriteResult] = Utils.retryOnce {
    collection.update(filter, elt, upsert = true)
  }

  def delete(collection: JSONCollection, filter: JsObject = Json.obj()): Future[WriteResult] = Utils.retryOnce {
    collection.remove(filter)
  }

  def findAll[T](collection: JSONCollection, filter: JsObject, query: String = "", filterFields: List[String] = Nil, sort: String = "")(implicit r: Reads[T]): Future[List[T]] = Utils.retryOnce {
    val mongoFilterJson = buildFilter(filter, query, filterFields)
    val mongoOrder = buildOrder(sort)

    collection.find(mongoFilterJson).sort(mongoOrder).cursor[T].collect[List]()
  }

  def findPage[T](collection: JSONCollection, filter: JsObject, query: String = "", filterFields: List[String] = Nil, page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "")(implicit r: Reads[T]): Future[Page[T]] = Utils.retryOnce {
    val realPage = if (page > 1) page - 1 else 0
    val offset = pageSize * realPage

    val mongoFilterJson = buildFilter(filter, query, filterFields)
    val mongoFilter = BSONFormats.BSONDocumentFormat.reads(mongoFilterJson).get
    val mongoOrder = buildOrder(sort)

    for (
      items <- collection.find(mongoFilterJson).options(QueryOpts(offset, pageSize)).sort(mongoOrder).cursor[T].collect[List](pageSize);
      totalItems <- collection.db.command(Count(collection.name, Some(mongoFilter)))
    ) yield Page(items, realPage + 1, pageSize, totalItems)
  }

  def getBy[T](uuid: String, collection: JSONCollection, fieldUuid: String = "uuid")(implicit r: Reads[T]): Future[Option[T]] = Utils.retryOnce {
    collection.find(Json.obj(fieldUuid -> uuid)).one[T]
  }

  def findBy[T](value: String, collection: JSONCollection, fieldName: String = "uuid")(implicit r: Reads[T]): Future[List[T]] = Utils.retryOnce {
    collection.find(Json.obj(fieldName -> value)).cursor[T].collect[List]()
  }

  def countFor(value: String, collection: JSONCollection, fieldName: String = "uuid"): Future[Int] = Utils.retryOnce {
    val mongoFilterJson = Json.obj(fieldName -> value)
    val mongoFilter = BSONFormats.BSONDocumentFormat.reads(mongoFilterJson).get
    collection.db.command(Count(collection.name, Some(mongoFilter)))
  }

  def findByList[T](uuids: Seq[String], collection: JSONCollection, fieldUuid: String = "uuid")(implicit r: Reads[T]): Future[List[T]] = Utils.retryOnce {
    if (uuids.length > 0) {
      val mongoFilter = Json.obj(fieldUuid -> Json.obj("$in" -> uuids))
      collection.find(mongoFilter).cursor[T].collect[List]()
    } else {
      Future(List())
    }
  }

  def countForList[T](values: Seq[String], collection: JSONCollection, fieldName: String = "uuid"): Future[Map[String, Int]] = {
    count(Json.obj(fieldName -> Json.obj("$in" -> values)), fieldName, collection)
  }

  def count[T](filter: JsObject, group: String, collection: JSONCollection): Future[Map[String, Int]] = Utils.retryOnce {
    val groupPredicate = Json.obj("_id" -> ("$" + group), "count" -> Json.obj("$sum" -> 1))
    val commandDoc = BSONDocument(
      "aggregate" -> collection.name,
      "pipeline" -> BSONArray(
        BSONDocument("$match" -> BSONFormats.BSONDocumentFormat.reads(filter).get),
        BSONDocument("$group" -> BSONFormats.BSONDocumentFormat.reads(groupPredicate).get)))

    collection.db.command(RawCommand(commandDoc)).map { result =>
      import play.modules.reactivemongo.json.BSONFormats._
      // {"result":[{"_id":"fcb07f0c-2ff3-4408-b83e-8f42d6a00112","count":1.0},{"_id":"3dac7440-f8e5-497d-8b0b-77127d16e1ae","count":2.0}],"ok":1.0}
      (Json.toJson(result) \ "result").as[List[JsObject]].map { obj =>
        ((obj \ "_id").as[String], (obj \ "count").as[Int])
      }.toMap
    }
  }

  def aggregate[T](collection: JSONCollection, command: JsObject)(implicit r: Reads[T]): Future[T] = Utils.retryOnce {
    collection.db.command(RawCommand(BSONFormats.BSONDocumentFormat.reads(command).get)).map { result =>
      import play.modules.reactivemongo.json.BSONFormats._
      (Json.toJson(result) \ "result").as[T]
    }
  }

  def aggregate2[T](collection: JSONCollection, command: JsObject, extract: JsValue => T): Future[T] = Utils.retryOnce {
    collection.db.command(RawCommand(BSONFormats.BSONDocumentFormat.reads(command).get)).map { result =>
      import play.modules.reactivemongo.json.BSONFormats._
      extract(Json.toJson(result) \ "result")
    }
  }

  def distinct(field: String, filter: JsObject = Json.obj(), collection: JSONCollection): Future[List[String]] = Utils.retryOnce {
    val commandDoc = BSONDocument(
      "distinct" -> collection.name,
      "key" -> field,
      "query" -> BSONFormats.BSONDocumentFormat.reads(filter).get)
    collection.db.command(RawCommand(commandDoc)).map { result =>
      import play.modules.reactivemongo.json.BSONFormats._
      (Json.toJson(result) \ "values").as[List[String]]
    }
  }

  def update[T](uuid: String, elt: T, collection: JSONCollection, fieldUuid: String = "uuid")(implicit w: Writes[T], ow: OWrites[T]): Future[UpdateWriteResult] = Utils.retryOnce {
    collection.update(Json.obj(fieldUuid -> uuid), elt)
  }

  def upsert[T](uuid: String, elt: T, collection: JSONCollection, fieldUuid: String = "uuid")(implicit w: Writes[T], ow: OWrites[T]): Future[UpdateWriteResult] = Utils.retryOnce {
    collection.update(Json.obj(fieldUuid -> uuid), elt, upsert = true)
  }

  def deleteBy(uuid: String, collection: JSONCollection, fieldUuid: String = "uuid"): Future[WriteResult] = Utils.retryOnce {
    collection.remove(Json.obj(fieldUuid -> uuid))
  }

  def bulkInsert[T](elts: List[T], collection: JSONCollection)(implicit w: Writes[T]): Future[MultiBulkWriteResult] = Utils.retryOnce {
    collection.bulkInsert(elts.map(e => w.writes(e).as[JsObject]).toStream, true)
  }

  // TODO : make a real bulk update !!!
  def bulkUpdate[T](elts: List[(String, T)], collection: JSONCollection, fieldUuid: String = "uuid")(implicit w: Writes[T], ow: OWrites[T]): Future[Int] = {
    val futureList = elts.map { elt => update(elt._1, elt._2, collection, fieldUuid) }
    Future.sequence(futureList).map { list =>
      list.filter(_.ok).length
    }
  }

  // TODO : make a real bulk upsert !!!
  def bulkUpsert[T](elts: List[(String, T)], collection: JSONCollection, fieldUuid: String = "uuid")(implicit w: Writes[T], ow: OWrites[T]): Future[Int] = {
    val futureList = elts.map { elt => upsert(elt._1, elt._2, collection, fieldUuid) }
    Future.sequence(futureList).map { list =>
      list.filter(_.ok).length
    }
  }

  def bulkDelete(uuids: List[String], collection: JSONCollection, fieldUuid: String = "uuid"): Future[WriteResult] = Utils.retryOnce {
    collection.remove(Json.obj(fieldUuid -> Json.obj("$in" -> uuids)))
  }

  def drop(collection: JSONCollection): Future[Boolean] = Utils.retryOnce {
    collection.db.command(new Drop(collection.name))
  }

  private def buildFilter(filter: JsObject, query: String, filterFields: List[String]): JsObject = {
    val jsonQuery = Json.obj("$or" -> filterFields.map(field => Json.obj(field -> Json.obj("$regex" -> (".*" + query + ".*"), "$options" -> "i"))))
    Json.obj("$and" -> List(filter, jsonQuery))
  }

  private def buildOrder(sort: String): JsObject = {
    val IsReverse = "(.)(.*)".r
    sort match {
      case IsReverse("-", v) => Json.obj(v -> -1)
      case IsReverse(v1, v2) => Json.obj((v1 + v2) -> 1)
      case v => Json.obj()
    }
  }
}
