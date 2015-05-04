package models

import org.joda.time.DateTime

case class Event(
  uuid: String,
  name: String,
  description: String,
  logo: String,
  start: DateTime,
  end: DateTime,
  address: Address,
  twitterHashtag: String,
  created: DateTime,
  updated: DateTime)
