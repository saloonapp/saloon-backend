package common.repositories.user

import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import common.models.user.Request
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

  //def get(uuid: String): Future[Option[Request]] = crud.getByUuid(uuid)
  def getPending(uuid: String): Future[Option[Request]] = crud.get(Json.obj("uuid" -> uuid, "status" -> Request.Status.pending))
  def getPendingAccountRequestByEmail(email: String): Future[Option[Request]] = crud.get(Json.obj("status" -> Request.Status.pending, "content.accountRequest" -> true, "content.email" -> email))
  //def getAccountRequest(uuid: String): Future[Option[Request]] = crud.get(Json.obj("uuid" -> uuid, "content.accountRequest" -> true))
  //def getPasswordReset(uuid: String): Future[Option[Request]] = crud.get(Json.obj("uuid" -> uuid, "content.passwordReset" -> true, "created" -> Json.obj("$gte" -> new DateTime().plusMinutes(-15))))
  //def getUserInvite(uuid: String): Future[Option[Request]] = crud.get(Json.obj("uuid" -> uuid, "content.userInvite" -> true))

  def insert(elt: Request): Future[LastError] = crud.insert(elt)
  def update(elt: Request): Future[LastError] = crud.update(elt.uuid, elt)
  def setAccepted(uuid: String): Future[LastError] = setStatus(uuid, Request.Status.accepted)
  //def setRejected(uuid: String): Future[LastError] = setStatus(uuid, Request.Status.rejected)
  private def setStatus(uuid: String, status: String): Future[LastError] = crud.update(Json.obj("uuid" -> uuid), Json.obj("$set" -> Json.obj("status" -> status, "updated" -> new DateTime())))
}
object RequestRepository extends MongoDbRequestRepository
