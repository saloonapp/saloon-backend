package infrastructure.repository.common

import models.common.Page
import scala.concurrent.Future

/*
 * Convention :
 *  - methods get* return one result (Option[T])
 *  - methods find* return a list of results (List[T])
 */
trait Repository[A] {
  def findAll(query: String = "", sort: String = ""): Future[List[A]]
  def findPage(query: String = "", page: Int = 1, pageSize: Int = Page.defaultSize, sort: String = ""): Future[Page[A]]
  def getByUuid(uuid: String): Future[Option[A]]
  def insert(elt: A): Future[Option[A]]
  def update(uuid: String, elt: A): Future[Option[A]]
  def delete(uuid: String): Future[Option[A]]
}
object Repository {
  def generateUuid(): String = java.util.UUID.randomUUID().toString()
}