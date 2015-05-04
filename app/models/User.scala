package models

import org.joda.time.DateTime

case class User(
  uuid: String,
  device: Device,
  created: DateTime,
  updated: DateTime)
