package common.repositories.user

import common.models.utils.Page
import common.models.values.typed.FullName
import common.models.user.User
import common.models.user.Organization
import common.models.user.OrganizationId
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbOrganizationRepository extends Repository[Organization, OrganizationId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.ORGANIZATIONS)

  private val crud = MongoDbCrudUtils(collection, Organization.format, List("name"), "uuid")

  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[Organization]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[Organization]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(organizationId: OrganizationId): Future[Option[Organization]] = crud.getByUuid(organizationId.unwrap)
  override def insert(organization: Organization): Future[Option[Organization]] = { crud.insert(organization).map(err => if (err.ok) Some(organization) else None) }
  override def update(organizationId: OrganizationId, organization: Organization): Future[Option[Organization]] = crud.update(organizationId.unwrap, organization).map(err => if (err.ok) Some(organization) else None)
  override def delete(organizationId: OrganizationId): Future[Option[Organization]] = {
    crud.delete(organizationId.unwrap).map { err =>
      UserRepository.removeOrganization(organizationId)
      None
    }
  }

  def getByName(name: FullName): Future[Option[Organization]] = crud.getBy("name", name.unwrap)
  def findByUuids(organizationIds: List[OrganizationId]): Future[List[Organization]] = crud.findByUuids(organizationIds.map(_.unwrap))
  def findAllowed(user: User): Future[List[Organization]] = if (user.canAdministrateSaloon()) findAll() else findByUuids(user.organizationIds.map(_.organizationId))
}
object OrganizationRepository extends MongoDbOrganizationRepository
