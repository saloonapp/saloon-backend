package common

import play.api.libs.json._

object Formatters {
  implicit def eitherFormatter[T, U](implicit fmtT: Format[T], fmtU: Format[U]): Format[Either[T, U]] = new Format[Either[T, U]] {
    override def reads(json: JsValue): JsResult[Either[T, U]] = (fmtT.reads(json), fmtU.reads(json)) match {
      case (JsSuccess(t, path), _) => JsSuccess(Left(t), path)
      case (_, JsSuccess(u, path)) => JsSuccess(Right(u), path)
      case (JsError(errors), _) => JsError(errors)
    }
    override def writes(o: Either[T, U]): JsValue = o.fold(fmtT.writes, fmtU.writes)
  }
}
