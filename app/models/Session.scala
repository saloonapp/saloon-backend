package models

import org.joda.time.DateTime

case class Session(
  uuid: String,
  eventId: String,
  title: String,
  summary: String,
  start: DateTime,
  end: DateTime,
  place: Place, // room, booth...
  format: String,
  category: String,
  speakerIds: List[String],
  tags: List[String],
  created: DateTime,
  updated: DateTime)
