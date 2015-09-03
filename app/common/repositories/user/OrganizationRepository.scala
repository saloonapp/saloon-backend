package common.repositories.user

import common.models.utils.Page
import common.models.user.User
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import common.models.user.Organization
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbOrganizationRepository extends Repository[Organization, String] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.ORGANIZATIONS)

  private val crud = MongoDbCrudUtils(collection, Organization.format, List("name"), "uuid")

  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Organization]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Organization]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(uuid: String): Future[Option[Organization]] = crud.getByUuid(uuid)
  override def insert(elt: Organization): Future[Option[Organization]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: Organization): Future[Option[Organization]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[Organization]] = {
    crud.delete(uuid).map { err =>
      UserRepository.removeOrganization(uuid)
      None
    }
  }

  def getByName(name: String): Future[Option[Organization]] = crud.getBy("name", name)
  def findByUuids(uuids: List[String]): Future[List[Organization]] = crud.findByUuids(uuids)
  def findAllowed(user: User): Future[List[Organization]] = if (user.canAdministrateSaloon()) findAll() else findByUuids(user.organizationIds.map(_.organizationId))
}
object OrganizationRepository extends MongoDbOrganizationRepository
