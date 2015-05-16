package infrastructure.repository.common

import models.common.Page
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.LastError
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
  def find(filter: JsObject = Json.obj()): Future[List[T]] = MongoDbCrudUtils.find(collection, filter)
  def get(filter: JsObject = Json.obj()): Future[Option[T]] = MongoDbCrudUtils.get(collection, filter)
  def insert(elt: T): Future[LastError] = MongoDbCrudUtils.insert(collection, elt)
  def update(filter: JsObject, elt: T): Future[LastError] = MongoDbCrudUtils.update(collection, filter, elt)
  def update(filter: JsObject, elt: JsObject): Future[LastError] = MongoDbCrudUtils.update(collection, filter, elt)
  def delete(filter: JsObject = Json.obj()): Future[LastError] = MongoDbCrudUtils.delete(collection, filter)
  def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[T]] = MongoDbCrudUtils.findAll(collection, filter, query, filterFields, sort)
  def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[T]] = MongoDbCrudUtils.findPage(collection, filter, query, filterFields, page, pageSize, sort)
  def findBy(fieldName: String, fieldValue: String): Future[List[T]] = MongoDbCrudUtils.findBy(fieldValue, collection, fieldName)
  def countFor(fieldName: String, fieldValue: String): Future[Int] = MongoDbCrudUtils.countFor(fieldValue, collection, fieldName)
  def getByUuid(uuid: String): Future[Option[T]] = MongoDbCrudUtils.getBy(uuid, collection, fieldUuid)
  def findByUuids(uuids: Seq[String]): Future[List[T]] = MongoDbCrudUtils.findByList(uuids, collection, fieldUuid)
  def getBy(fieldName: String, fieldValue: String): Future[Option[T]] = MongoDbCrudUtils.getBy(fieldValue, collection, fieldName)
  def findBy(fieldName: String, fieldValues: Seq[String]): Future[List[T]] = MongoDbCrudUtils.findByList(fieldValues, collection, fieldName)
  def countFor(fieldName: String, fieldValues: Seq[String]): Future[Map[String, Int]] = MongoDbCrudUtils.countForList(fieldValues, collection, fieldName)
  def update(uuid: String, elt: T): Future[LastError] = MongoDbCrudUtils.update(uuid, elt, collection, fieldUuid)
  def delete(uuid: String): Future[LastError] = MongoDbCrudUtils.deleteBy(uuid, collection, fieldUuid)
  def deleteBy(fieldName: String, fieldValue: String): Future[LastError] = MongoDbCrudUtils.deleteBy(fieldValue, collection, fieldName)
  def bulkInsert(elts: List[T]): Future[Int] = MongoDbCrudUtils.bulkInsert(elts, collection)
  def drop(): Future[Boolean] = MongoDbCrudUtils.drop(collection)
}
object MongoDbCrudUtils {
  def find[T](collection: JSONCollection, filter: JsObject = Json.obj())(implicit r: Reads[T]): Future[List[T]] = {
    collection.find(filter).cursor[T].collect[List]()
  }

  def get[T](collection: JSONCollection, filter: JsObject = Json.obj())(implicit r: Reads[T]): Future[Option[T]] = {
    collection.find(filter).one[T]
  }

  def insert[T](collection: JSONCollection, elt: T)(implicit w: Writes[T]): Future[LastError] = {
    collection.save(elt)
  }

  def update[T](collection: JSONCollection, filter: JsObject, elt: T)(implicit w: Writes[T]): Future[LastError] = {
    collection.update(filter, elt)
  }

  def update[T](collection: JSONCollection, filter: JsObject, elt: JsObject): Future[LastError] = {
    collection.update(filter, elt)
  }

  def delete(collection: JSONCollection, filter: JsObject = Json.obj()): Future[LastError] = {
    collection.remove(filter)
  }

  def findAll[T](collection: JSONCollection, filter: JsObject, query: String = "", filterFields: List[String] = Nil, sort: String = "")(implicit r: Reads[T]): Future[List[T]] = {
    val mongoFilterJson = buildFilter(filter, query, filterFields)
    val mongoOrder = buildOrder(sort)

    collection.find(mongoFilterJson).sort(mongoOrder).cursor[T].collect[List]()
  }

  def findPage[T](collection: JSONCollection, filter: JsObject, query: String = "", filterFields: List[String] = Nil, page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "")(implicit r: Reads[T]): Future[Page[T]] = {
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

  def getBy[T](uuid: String, collection: JSONCollection, fieldUuid: String = "uuid")(implicit r: Reads[T]): Future[Option[T]] = {
    collection.find(Json.obj(fieldUuid -> uuid)).one[T]
  }

  def findBy[T](value: String, collection: JSONCollection, fieldName: String = "uuid")(implicit r: Reads[T]): Future[List[T]] = {
    collection.find(Json.obj(fieldName -> value)).cursor[T].collect[List]()
  }

  def countFor(value: String, collection: JSONCollection, fieldName: String = "uuid"): Future[Int] = {
    val mongoFilterJson = Json.obj(fieldName -> value)
    val mongoFilter = BSONFormats.BSONDocumentFormat.reads(mongoFilterJson).get
    collection.db.command(Count(collection.name, Some(mongoFilter)))
  }

  def findByList[T](uuids: Seq[String], collection: JSONCollection, fieldUuid: String = "uuid")(implicit r: Reads[T]): Future[List[T]] = {
    val mongoFilter = Json.obj("$or" -> uuids.map(uuid => Json.obj(fieldUuid -> uuid)))
    collection.find(mongoFilter).cursor[T].collect[List]()
  }

  def countForList[T](values: Seq[String], collection: JSONCollection, fieldName: String = "uuid"): Future[Map[String, Int]] = {
    val matchPredicate = Json.obj(fieldName -> Json.obj("$in" -> values))
    val groupPredicate = Json.obj("_id" -> ("$" + fieldName), "count" -> Json.obj("$sum" -> 1))
    val commandDoc = BSONDocument(
      "aggregate" -> collection.name,
      "pipeline" -> BSONArray(
        BSONDocument("$match" -> BSONFormats.BSONDocumentFormat.reads(matchPredicate).get),
        BSONDocument("$group" -> BSONFormats.BSONDocumentFormat.reads(groupPredicate).get)))

    collection.db.command(RawCommand(commandDoc)).map { result =>
      import play.modules.reactivemongo.json.BSONFormats._
      // {"result":[{"_id":"fcb07f0c-2ff3-4408-b83e-8f42d6a00112","count":1.0},{"_id":"3dac7440-f8e5-497d-8b0b-77127d16e1ae","count":2.0}],"ok":1.0}
      (Json.toJson(result) \ "result").as[List[JsObject]].map { obj =>
        ((obj \ "_id").as[String], (obj \ "count").as[Int])
      }.toMap
    }
  }

  def update[T](uuid: String, elt: T, collection: JSONCollection, fieldUuid: String = "uuid")(implicit w: Writes[T]): Future[LastError] = {
    collection.update(Json.obj(fieldUuid -> uuid), elt)
  }

  def deleteBy(uuid: String, collection: JSONCollection, fieldUuid: String = "uuid"): Future[LastError] = {
    collection.remove(Json.obj(fieldUuid -> uuid))
  }

  def bulkInsert[T](elts: List[T], collection: JSONCollection)(implicit w: Writes[T]): Future[Int] = {
    collection.bulkInsert(Enumerator.enumerate(elts))
  }

  def drop(collection: JSONCollection): Future[Boolean] = {
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