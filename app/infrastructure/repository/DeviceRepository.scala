package infrastructure.repository

import common.models.Page
import common.infrastructure.repository.Repository
import common.infrastructure.repository.MongoDbCrudUtils
import models.user.Device
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbDeviceRepository extends Repository[Device] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.DEVICES)
  lazy val userCollection: JSONCollection = db[JSONCollection](CollectionReferences.USERS)

  private val crud = MongoDbCrudUtils(collection, Device.format, List("info.uuid", "info.platform", "info.manufacturer", "info.model", "info.version", "info.cordova", "pushId", "saloonMemo"), "uuid")

  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Device]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Device]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[Device]] = crud.getByUuid(uuid)
  override def insert(elt: Device): Future[Option[Device]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Device): Future[Option[Device]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Device]] = {
    crud.delete(uuid).map { err =>
      UserActionRepository.deleteByUser(uuid)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(uuids: List[String]): Future[List[Device]] = crud.findByUuids(uuids)
  def getByDeviceId(deviceId: String): Future[Option[Device]] = crud.getBy("info.uuid", deviceId)
  def bulkInsert(elts: List[Device]): Future[Int] = crud.bulkInsert(elts)
}
object DeviceRepository extends MongoDbDeviceRepository
