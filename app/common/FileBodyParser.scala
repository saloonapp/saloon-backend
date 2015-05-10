package common

import java.io.ByteArrayOutputStream
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee.Iteratee
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse.multipartFormData
import play.api.mvc.BodyParsers.parse.Multipart.PartHandler
import play.api.mvc.BodyParsers.parse.Multipart.handleFilePart
import play.api.mvc.BodyParsers.parse.Multipart.FileInfo
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart

object FileBodyParser {
  private def handleFilePartAsByteArray: PartHandler[FilePart[Array[Byte]]] =
    handleFilePart {
      case FileInfo(partName, filename, contentType) =>
        // simply write the data to the ByteArrayOutputStream
        val iteratee = Iteratee.fold[Array[Byte], ByteArrayOutputStream](new ByteArrayOutputStream()) { (os, data) =>
          os.write(data)
          os
        }
        // Iteratee[ByteArrayOutputStream] => Iteratee[Array[Byte]]
        iteratee.map { os =>
          os.close()
          os.toByteArray
        }
    }

  def multipartFormDataAsBytes: BodyParser[MultipartFormData[Array[Byte]]] = multipartFormData(handleFilePartAsByteArray)
}