package org.cheminot.web

import org.joda.time.DateTime
import rapture.http._
import rapture.mime.MimeTypes
import rapture.mime.MimeTypes.MimeType
import org.cheminot.misc

package object router {

  object AsDateTime {
    import org.joda.time.DateTime
    def unapply(s: String): Option[DateTime] =
      misc.DateTime.parse(s)
  }

  def ContentNegotiation[A](request: HttpRequest)(f: MimeType => A): A = {
    val mime = request.headers.get("Accept")
      .flatMap(_.headOption.flatMap(_.split(",").headOption))
      .map { case MimeTypes(m) => m }
      .getOrElse(MimeTypes.`text/html`)
    f(mime)
  }
}
