package common.repositories.user

import common.models.values.typed.Email
import common.models.values.typed.RequestStatus
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
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.commands.UpdateWriteResult
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbRequestRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.REQUESTS)

  private val crud = MongoDbCrudUtils(collection, Request.format, List("action.text"), "uuid")

  //def getByUuid(requestId: RequestId): Future[Option[Request]] = crud.getByUuid(requestId)
  def getPending(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "status" -> RequestStatus.pending.unwrap))
  def getPendingByUser(requestId: RequestId, userId: UserId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "status" -> RequestStatus.pending.unwrap, "userId" -> userId))
  def getPendingAccountRequestByEmail(email: Email): Future[Option[Request]] = crud.get(Json.obj("status" -> RequestStatus.pending.unwrap, "content.accountRequest" -> true, "content.email" -> email.unwrap))
  //def getAccountRequest(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "content.accountRequest" -> true))
  def getPendingInviteForRequest(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("status" -> RequestStatus.pending.unwrap, "content.accountInvite" -> true, "content.next" -> requestId))
  def getPasswordReset(requestId: RequestId): Future[Option[Request]] = crud.get(Json.obj("uuid" -> requestId, "content.passwordReset" -> true, "status" -> RequestStatus.pending.unwrap, "created" -> Json.obj("$gte" -> new DateTime().plusMinutes(-30))))
  def findPendingOrganizationRequestsByUser(userId: UserId): Future[List[Request]] = crud.find(Json.obj("userId" -> userId, "status" -> RequestStatus.pending.unwrap, "content.organizationRequest" -> true))
  def findPendingOrganizationInvitesByEmail(email: Email): Future[List[Request]] = crud.find(Json.obj("content.email" -> email.unwrap, "status" -> RequestStatus.pending.unwrap, "content.organizationInvite" -> true))
  def findPendingOrganizationRequestsByOrganization(organizationId: OrganizationId): Future[List[Request]] = crud.find(Json.obj("status" -> RequestStatus.pending.unwrap, "content.organizationRequest" -> true, "content.organizationId" -> organizationId))
  def findPendingOrganizationInvitesByOrganization(organizationId: OrganizationId): Future[List[Request]] = crud.find(Json.obj("status" -> RequestStatus.pending.unwrap, "content.organizationInvite" -> true, "content.organizationId" -> organizationId))
  def countPendingOrganizationRequestsFor(organizationIds: List[OrganizationId]): Future[Map[OrganizationId, Int]] = crud.count(Json.obj("status" -> RequestStatus.pending.unwrap, "content.organizationRequest" -> true, "content.organizationId" -> Json.obj("$in" -> organizationIds)), "content.organizationId").map { _.map { case (key, value) => (OrganizationId(key), value) } }

  def insert(request: Request): Future[WriteResult] = crud.insert(request)
  def update(request: Request): Future[UpdateWriteResult] = crud.update(request.uuid.unwrap, request)
  def setAccepted(requestId: RequestId): Future[UpdateWriteResult] = setStatus(requestId, RequestStatus.accepted)
  def setCanceled(requestId: RequestId): Future[UpdateWriteResult] = setStatus(requestId, RequestStatus.canceled)
  def setRejected(requestId: RequestId): Future[UpdateWriteResult] = setStatus(requestId, RequestStatus.rejected)
  private def setStatus(requestId: RequestId, status: RequestStatus): Future[UpdateWriteResult] = crud.update(Json.obj("uuid" -> requestId), Json.obj("$set" -> Json.obj("status" -> status.unwrap, "updated" -> new DateTime())))

  // TODO : def incrementVisited(requestId: String): Future[UpdateWriteResult] cf Auth.createAccount
}
object RequestRepository extends MongoDbRequestRepository
