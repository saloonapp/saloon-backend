package authentication.repositories.impl

import common.models.user.User
import common.repositories.CollectionReferences
import authentication.repositories.UserRepository
import authentication.repositories.UserCreationException
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.Play.current
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import com.mohiva.play.silhouette.core.LoginInfo

object MongoUserRepository extends UserRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERS)
  implicit val formatLoginInfo = Json.format[LoginInfo]

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    play.Logger.info("find User for loginInfo: " + loginInfo)
    collection.find(Json.obj("loginInfo" -> loginInfo)).one[User]
  }

  def save(user: User) = {
    play.Logger.info("save User: " + user)
    collection.find(Json.obj("email" -> user.email)).one[User].flatMap {
      _.map { existingUser => // user exists
        //Future.failed(new UserCreationException("user email already exists."))
        Future(existingUser)
      }.getOrElse { // user does not exists
        collection.save(user).map { err =>
          user
        }
      }
    }
  }

  def remove(loginInfo: LoginInfo): Future[LastError] = collection.remove(Json.obj("loginInfo" -> loginInfo))
}
