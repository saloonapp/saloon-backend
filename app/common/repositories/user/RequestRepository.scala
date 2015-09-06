package common.repositories.user

import common.models.user.Request
import common.models.user.RequestId
import common.models.user.UserId
import common.models.user.OrganizationId
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbRequestRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.REQUESTS)

  private val crud = MongoDbCrudUtils(collection, Request.format, List("action.text"), "uuid")

  //def getByUuid(requestId: RequestId): Future[Option[Request]] = crud.getByUuid(requestId)
  def getPending(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "status" -> Request.Status.pending))
  def getPendingByUser(requestId: RequestId, userId: UserId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "status" -> Request.Status.pending, "userId" -> userId))
  def getPendingAccountRequestByEmail(email: String): Future[Option[Request]] = crud.get(Json.obj("status" -> Request.Status.pending, "content.accountRequest" -> true, "content.email" -> email))
  //def getAccountRequest(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "content.accountRequest" -> true))
  def getPendingInviteForRequest(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("status" -> Request.Status.pending, "content.accountInvite" -> true, "content.next" -> requestId))
  //def getPasswordReset(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "content.passwordReset" -> true, "created" -> Json.obj("$gte" -> new DateTime().plusMinutes(-15))))
  def findPendingOrganizationRequestsByUser(userId: UserId): Future[List[Request]] = crud.find(Json.obj("userId" -> userId, "status" -> Request.Status.pending, "content.organizationRequest" -> true))
  def findPendingOrganizationInvitesByEmail(email: String): Future[List[Request]] = crud.find(Json.obj("content.email" -> email, "status" -> Request.Status.pending, "content.organizationInvite" -> true))
  def findPendingOrganizationRequestsByOrganization(organizationId: OrganizationId): Future[List[Request]] = crud.find(Json.obj("status" -> Request.Status.pending, "content.organizationRequest" -> true, "content.organizationId" -> organizationId))
  def findPendingOrganizationInvitesByOrganization(organizationId: OrganizationId): Future[List[Request]] = crud.find(Json.obj("status" -> Request.Status.pending, "content.organizationInvite" -> true, "content.organizationId" -> organizationId))
  def countPendingOrganizationRequestsFor(organizationIds: List[OrganizationId]): Future[Map[OrganizationId, Int]] = crud.count(Json.obj("status" -> Request.Status.pending, "content.organizationRequest" -> true, "content.organizationId" -> Json.obj("$in" -> organizationIds)), "content.organizationId").map { _.map { case (key, value) => (OrganizationId(key), value) } }

  def insert(request: Request): Future[LastError] = crud.insert(request)
  def update(request: Request): Future[LastError] = crud.update(request.uuid.unwrap, request)
  def setAccepted(requestId: RequestId): Future[LastError] = setStatus(requestId, Request.Status.accepted)
  def setCanceled(requestId: RequestId): Future[LastError] = setStatus(requestId, Request.Status.canceled)
  def setRejected(requestId: RequestId): Future[LastError] = setStatus(requestId, Request.Status.rejected)
  private def setStatus(requestId: RequestId, status: String): Future[LastError] = crud.update(Json.obj("uuid" -> requestId), Json.obj("$set" -> Json.obj("status" -> status, "updated" -> new DateTime())))

  // TODO : def incrementVisited(requestId: String): Future[LastError] cf Auth.createAccount
}
object RequestRepository extends MongoDbRequestRepository
