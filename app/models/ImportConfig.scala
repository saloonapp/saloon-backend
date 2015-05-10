package models

import play.api.mvc.Request
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile
import play.api.data.Forms._

case class ImportConfig(
  shouldClean: Boolean,
  importedFile: Option[java.io.File]) {
  def withFile()(implicit req: Request[MultipartFormData[TemporaryFile]]): Option[ImportConfig] = {
    import java.io.File
    req.body.file("importedFile").map { data =>
      val file = new File(play.Play.application().path().getAbsolutePath + "/upload/" + System.currentTimeMillis + "_" + data.filename)
      data.ref.moveTo(file)
      this.copy(importedFile = Some(file))
    }
  }
}
object ImportConfig {
  val fields = mapping(
    "shouldClean" -> boolean,
    "importedFile" -> ignored(Option.empty[java.io.File]))(ImportConfig.apply)(ImportConfig.unapply)
}