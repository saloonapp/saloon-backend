package common.repositories.user

import common.models.utils.Page
import common.models.user.Device
import common.models.user.DeviceId
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.commands.MultiBulkWriteResult
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbDeviceRepository extends Repository[Device, DeviceId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.DEVICES)

  private val crud = MongoDbCrudUtils(collection, Device.format, List("info.uuid", "info.platform", "info.manufacturer", "info.model", "info.version", "info.cordova", "pushId", "saloonMemo"), "uuid")

  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Device]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Device]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(deviceId: DeviceId): Future[Option[Device]] = crud.getByUuid(deviceId.unwrap)
  override def insert(device: Device): Future[Option[Device]] = { crud.insert(device).map(err => if (err.ok) Some(device) else None) }
  override def update(deviceId: DeviceId, device: Device): Future[Option[Device]] = crud.update(deviceId.unwrap, device).map(err => if (err.ok) Some(device) else None)
  override def delete(deviceId: DeviceId): Future[Option[Device]] = {
    crud.delete(deviceId.unwrap).map { err =>
      UserActionRepository.deleteByUser(deviceId)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(deviceIds: List[DeviceId]): Future[List[Device]] = crud.findByUuids(deviceIds.map(_.unwrap))
  def getByDeviceId(deviceUuid: String): Future[Option[Device]] = crud.getBy("info.uuid", deviceUuid)
  def bulkInsert(devices: List[Device]): Future[MultiBulkWriteResult] = crud.bulkInsert(devices)
}
object DeviceRepository extends MongoDbDeviceRepository
