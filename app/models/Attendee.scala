package models

import org.joda.time.DateTime

case class Attendee(
  uuid: String,
  eventId: String,
  firstName: String,
  lastName: String,
  role: String,
  bio: String,
  avatar: String,
  company: String,
  site: String,
  created: DateTime,
  updated: DateTime)
