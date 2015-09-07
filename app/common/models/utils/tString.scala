package common.models.utils

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.data.validation.ValidationError
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.libs.json.JsValue
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsPath
import play.api.libs.json.JsString
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Format
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable
import play.api.mvc.JavascriptLitteral

/*
 * tString is for typed String
 * 
 * The purpose of this class is to allow to easily transform String into typed object to improve the type safety of your play app via Value Classes.
 * It creates all the necessary bindings to work with router, form, json...
 * 
 * Ex: 
 * 
 * case class Prenom(val value: String) extends AnyVal with tString {
 *   def unwrap: String = this.value
 * }
 * object Prenom extends tStringHelper[Prenom] {
 *   protected def build(str: String): Option[Prenom] = Some(Prenom(str))
 * }
 */

trait tString extends Any {
  def unwrap: String
  def isEmpty: Boolean = this.unwrap.isEmpty
  override def toString: String = this.unwrap
}
trait tStringHelper[T <: tString] {
  def build(str: String): Either[String, T]
  protected val buildErrKey = "error.wrongFormat"
  protected val buildErrMsg = "Wrong format"
  implicit val pathBinder = new PathBindable[T] {
    override def bind(key: String, value: String): Either[String, T] = build(value)
    override def unbind(key: String, value: T): String = value.unwrap
  }
  implicit val queryBinder = new QueryStringBindable[T] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = params.get(key).map { values =>
      values match {
        case v :: vs => build(v)
        case _ => Left(buildErrMsg)
      }
    }
    override def unbind(key: String, value: T): String = value.unwrap
  }
  implicit val javascriptBinder = new JavascriptLitteral[T] {
    def to(value: T): String = value.unwrap
  }
  implicit val jsonFormat = Format(new Reads[T] {
    override def reads(json: JsValue): JsResult[T] = json.validate[String].flatMap(id => build(id) match {
      case Right(uuid) => JsSuccess(uuid)
      case Left(err) => JsError(Seq((JsPath(List()), Seq(ValidationError(err)))))
    })
  }, new Writes[T] {
    override def writes(value: T): JsValue = JsString(value.unwrap)
  })
  implicit val formMapping = new Formatter[T] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T] = data.get(key).map { value => build(value).left.map(msg => Seq(FormError(key, msg, Nil))) }.getOrElse(Left(Seq(FormError(key, buildErrKey, Nil))))
    override def unbind(key: String, value: T): Map[String, String] = Map(key -> value.unwrap)
  }
}
object tStringConstraints {
  /*
   * cf :
   * 	- https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/data/validation/Validation.scala
   * 	- https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/data/Form.scala
   * 	- https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/data/Forms.scala
   */
  def nonEmpty: Constraint[tString] = Constraint[tString]("constraint.required") { o =>
    if (o == null) Invalid(ValidationError("error.required")) else if (o.unwrap.trim.isEmpty) Invalid(ValidationError("error.required")) else Valid
  }
}
