package models

import play.api.data.Forms._

case class ImportConfig(
  shouldClean: Boolean)
object ImportConfig {
  val fields = mapping(
    "shouldClean" -> boolean,
    "importedFile" -> ignored(None))((shouldClean, file) => ImportConfig(shouldClean))(importConfig => Some(importConfig.shouldClean, None))
}
