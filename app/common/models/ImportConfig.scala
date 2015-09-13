package common.models

import common.models.values.typed.WebsiteUrl
import play.api.data.Forms._

case class FileImportConfig(
  encoding: String = FileImportConfig.encodings(0)._1,
  shouldClean: Boolean = true)
object FileImportConfig {
  val encodings = Seq(("UTF-8", "UTF-8 (default)"), ("CP1252", "CP1252 (windows)"))
  val fields = mapping(
    "encoding" -> nonEmptyText,
    "shouldClean" -> boolean,
    "importedFile" -> ignored(None))((encoding, shouldClean, file) => FileImportConfig(encoding, shouldClean))(importConfig => Some(importConfig.encoding, importConfig.shouldClean, None))
}

case class UrlImportConfig(
  url: WebsiteUrl)
object UrlImportConfig {
  val fields = mapping(
    "url" -> of[WebsiteUrl])(UrlImportConfig.apply)(UrlImportConfig.unapply)
}
