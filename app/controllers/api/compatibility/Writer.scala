package controllers.api.compatibility

import models.event.Event
import models.event.Session
import models.event.Exponent
import models.user.Device
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
  //def write(data: Session): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Session.className)
  def write(data: Session): JsObject = Json.obj(
    "uuid" -> data.uuid,
    "eventId" -> data.eventId,
    "name" -> data.name,
    "description" -> data.description,
    "format" -> data.info.format,
    "category" -> data.info.category,
    "place" -> data.info.place,
    "start" -> data.info.start,
    "end" -> data.info.end,
    "speakers" -> data.info.speakers,
    "tags" -> Json.arr(),
    "created" -> data.meta.created,
    "updated" -> data.meta.updated,
    "className" -> Session.className)
  //def write(data: Exponent): JsObject = Json.toJson(data).as[JsObject] ++ Json.obj("className" -> Exponent.className)
  def write(data: Exponent): JsObject = Json.obj(
    "uuid" -> data.uuid,
    "eventId" -> data.eventId,
    "name" -> data.name,
    "description" -> data.description,
    "logoUrl" -> data.images.logo,
    "landingUrl" -> data.images.landing,
    "siteUrl" -> data.info.website,
    "team" -> data.info.team,
    "level" -> data.info.level,
    "sponsor" -> data.info.sponsor,
    "tags" -> Json.arr(),
    "images" -> Json.arr(),
    "created" -> data.meta.created,
    "updated" -> data.meta.updated,
    "className" -> Exponent.className)
  //def write(data: Device): JsObject = Json.toJson(data).as[JsObject]
  def write(data: Device): JsObject = Json.obj(
    "uuid" -> data.uuid,
    "device" -> Json.obj(
      "uuid" -> data.info.uuid,
      "platform" -> data.info.platform,
      "manufacturer" -> data.info.manufacturer,
      "model" -> data.info.model,
      "version" -> data.info.version,
      "cordova" -> data.info.cordova),
    "saloonMemo" -> data.saloonMemo,
    "created" -> data.meta.created,
    "updated" -> data.meta.updated)

  def write(data: (Event, Int, Int)): JsObject = write(data._1) ++ Json.obj("sessionCount" -> data._2, "exponentCount" -> data._3)
}