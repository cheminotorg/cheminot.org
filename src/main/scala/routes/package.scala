package org.cheminot

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import rapture.http._
import rapture.mime.MimeTypes
import rapture.mime.MimeTypes.MimeType

package object router {

  object AsDateTime {
    import org.joda.time.DateTime
    def unapply(s: String): Option[DateTime] =
      scala.util.Try(ISODateTimeFormat.dateTime.parseDateTime(s)).toOption
  }

  def ContentNegotiation[A](request: HttpRequest)(f: MimeType => A): A = {
    val mime = request.headers.get("Accept")
      .flatMap(_.headOption.flatMap(_.split(",").headOption))
      .map { case MimeTypes(m) => m }
      .getOrElse(MimeTypes.`text/html`)
    f(mime)
  }
}
