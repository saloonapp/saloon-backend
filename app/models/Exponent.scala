package models

import org.joda.time.DateTime

case class Exponent(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  company: String,
  place: Place, // room, booth...
  tags: List[String],
  created: DateTime,
  updated: DateTime)
