package common.repositories

import common.models.values.UUID
import common.models.utils.Page
import scala.concurrent.Future
import play.api.libs.json._

/*
 * Convention :
 *  - methods get* return one result (Option[T])
 *  - methods find* return a list of results (List[T])
 */
trait Repository[A, AID] {
  def findAll(query: String = "", sort: String = "", filter: JsObject = Json.obj()): Future[List[A]]
  def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = "", filter: JsObject = Json.obj()): Future[Page[A]]
  def getByUuid(uuid: AID): Future[Option[A]]
  def insert(elt: A): Future[Option[A]]
  def update(uuid: AID, elt: A): Future[Option[A]]
  def delete(uuid: AID): Future[Option[A]]
}
object Repository {
  def generateUuid(): String = java.util.UUID.randomUUID().toString()
}