package controllers.api.compatibility

import models._
import play.api.libs.json._

object Writer {
  //def write(data: Event): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Event.className)
  def write(data: Event): JsObject = Json.obj(
    "uuid" -> data.uuid,
    "refreshUrl" -> data.meta.refreshUrl,
    "name" -> data.name,
    "description" -> data.description,
    "logoUrl" -> data.images.logo,
    "landingUrl" -> data.images.landing,
    "siteUrl" -> data.info.website,
    "start" -> data.info.start,
    "end" -> data.info.end,
    "address" -> data.info.address,
    "price" -> data.info.price.label,
    "priceUrl" -> data.info.price.url,
    "twitterHashtag" -> data.info.social.twitter.hashtag,
    "twitterAccount" -> data.info.social.twitter.account,
    "tags" -> data.meta.categories,
    "published" -> data.config.published,
    "created" -> data.meta.created,
    "updated" -> data.meta.updated,
    "className" -> Event.className)
  def write(data: Session): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Session.className)
  def write(data: Exponent): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Exponent.className)

  def write(data: (Event, Int, Int)): JsObject = write(data._1) ++ Json.obj("sessionCount" -> data._2, "exponentCount" -> data._3)
}