package common.repositories.user

import common.models.values.typed.Email
import common.models.values.typed.UserRole
import common.models.user.User
import common.models.user.UserId
import common.models.user.OrganizationId
import common.models.utils.Page
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import authentication.repositories.impl.MongoUserRepository
import authentication.repositories.impl.MongoPasswordRepository
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.commands.UpdateWriteResult
import play.modules.reactivemongo.json.JsObjectDocumentWriter
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbUserRepository extends Repository[User, UserId] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERS)

  private val crud = MongoDbCrudUtils(collection, User.format, List("email", "info.firstName", "info.lastName"), "uuid")

  //def findAllOld(): Future[List[UserOld]] = collection.find(Json.obj()).cursor[UserOld].collect[List]()
  override def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[User]] = crud.findAll(query, sort, filter)
  override def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[User]] = crud.findPage(query, page, pageSize, sort, filter)
  override def getByUuid(userId: UserId): Future[Option[User]] = crud.getByUuid(userId.unwrap)
  override def insert(user: User): Future[Option[User]] = { crud.insert(user).map(err => if (err.ok) Some(user) else None) }
  override def update(userId: UserId, user: User): Future[Option[User]] = crud.update(userId.unwrap, user).map(err => if (err.ok) Some(user) else None)
  override def delete(userId: UserId): Future[Option[User]] = {
    getByUuid(userId).flatMap { userOpt =>
      userOpt.map { user =>
        for {
          userRemoved <- MongoUserRepository.remove(user.loginInfo)
          passwordRemoved <- MongoPasswordRepository.remove(user.loginInfo)
        } yield userOpt
      }.getOrElse(Future(None))
    }
  }

  def getByEmail(email: Email): Future[Option[User]] = crud.get(Json.obj("email" -> email.unwrap))
  def findByUuids(userIds: List[UserId]): Future[List[User]] = crud.findByUuids(userIds.map(_.unwrap))
  def findByEmails(emails: List[Email]): Future[List[User]] = crud.findBy("email", emails.map(_.unwrap))
  def getOrganizationOwner(organizationId: OrganizationId): Future[Option[User]] = crud.get(Json.obj("organizationIds" -> Json.obj("organizationId" -> organizationId, "role" -> UserRole.owner.unwrap)))
  def findOrganizationMembers(organizationId: OrganizationId): Future[List[User]] = crud.find(Json.obj("organizationIds.organizationId" -> organizationId))
  def removeOrganization(organizationId: OrganizationId): Future[UpdateWriteResult] = collection.update(Json.obj("organizationIds.organizationId" -> organizationId), Json.obj("$pull" -> Json.obj("organizationIds" -> Json.obj("organizationId" -> organizationId))), multi = true)
}
object UserRepository extends MongoDbUserRepository
