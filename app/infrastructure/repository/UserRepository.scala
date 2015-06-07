package infrastructure.repository

import common.models.Page
import common.infrastructure.repository.Repository
import common.infrastructure.repository.MongoDbCrudUtils
import models.User
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbUserRepository extends Repository[User] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERS)

  private val crud = MongoDbCrudUtils(collection, User.format, List("device.platform", "device.manufacturer", "device.model", "device.version", "device.cordova", "push.platform", "saloonMemo"), "uuid")

  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[User]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[User]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[User]] = crud.getByUuid(uuid)
  override def insert(elt: User): Future[Option[User]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: User): Future[Option[User]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[User]] = {
    crud.delete(uuid).map { err =>
      UserActionRepository.deleteByUser(uuid)
      None
    } // TODO : return deleted elt !
  }

  def findByUuids(uuids: List[String]): Future[List[User]] = crud.findByUuids(uuids)
  def getByDevice(deviceId: String): Future[Option[User]] = crud.getBy("device.uuid", deviceId)
}
object UserRepository extends MongoDbUserRepository
