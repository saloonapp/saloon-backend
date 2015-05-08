package models.common

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

case class Page[A](items: Seq[A], currentPage: Int, pageSize: Int, totalItems: Long) {
  import java.math
  lazy val prev: Option[Int] = Option(currentPage - 1).filter(_ >= 1)
  lazy val next: Option[Int] = Option(currentPage + 1).filter(_ => ((currentPage - 1) * pageSize + items.size) < totalItems)
  lazy val totalPages: Int = Math.ceil(totalItems.toDouble / pageSize.toDouble).toInt
  def map[B](f: (A) => B): Page[B] = this.copy(items = items.map(f))
  def mapAsync[B](f: (A) => Future[B]): Future[Page[B]] = Future.sequence(items.map(f)).map(newItems => this.copy(items = newItems))
  def batchMap[B](f: (Seq[A]) => Seq[B]): Page[B] = this.copy(items = f(items))
  def batchMapAsync[B](f: (Seq[A]) => Future[Seq[B]]): Future[Page[B]] = f(items).map(newItems => this.copy(items = newItems))
}
object Page {
  val defaultSize: Int = 10
  implicit def format[T: Format] = new Format[Page[T]] {
    val tFormatter: Format[T] = implicitly[Format[T]]
    def reads(js: JsValue): JsResult[Page[T]] = {
      JsSuccess(Page[T](
        (js \ "items").as[Seq[T]],
        (js \ "currentPage").as[Int],
        (js \ "pageSize").as[Int],
        (js \ "totalItems").as[Long]))
    }
    def writes(p: Page[T]): JsValue = {
      Json.obj(
        "items" -> p.items,
        "currentPage" -> p.currentPage,
        "pageSize" -> p.pageSize,
        "totalItems" -> p.totalItems)
    }
  }
}