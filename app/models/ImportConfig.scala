package models

import play.api.data.Forms._

case class FileImportConfig(
  shouldClean: Boolean)
object FileImportConfig {
  val fields = mapping(
    "shouldClean" -> boolean,
    "importedFile" -> ignored(None))((shouldClean, file) => FileImportConfig(shouldClean))(importConfig => Some(importConfig.shouldClean, None))
}

case class UrlImportConfig(
  url: String,
  replaceIds: Boolean,
  newIds: Boolean)
object UrlImportConfig {
  val fields = mapping(
    "url" -> nonEmptyText,
    "replaceIds" -> boolean,
    "newIds" -> boolean)(UrlImportConfig.apply)(UrlImportConfig.unapply)
}
