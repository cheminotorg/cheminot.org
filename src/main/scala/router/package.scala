package org.cheminot.web

import rapture.codec._
import rapture.uri.RootedPath
import rapture.html.HtmlDoc
import rapture.json._, jsonBackends.jawn._
import rapture.http._
import rapture.mime.MimeTypes
import rapture.mime.MimeTypes.MimeType
import encodings.`UTF-8`._

package object router {

  def htmlJsonHandler[A <: Params](path: RootedPath)(getParams: HttpRequest => Option[A])(htmlHandler: A => HtmlDoc)(jsonHandler: A => Json): PartialFunction[HttpRequest, Response] = {
    import rapture.json.formatters.compact._
    import rapture.core.decimalFormats.exact._

    val jsonPath = {
      val jsonElement = s"${path.elements.last}.json"
      RootedPath(path.elements.init :+ jsonElement)
    }

    def withParams(req: HttpRequest)(handler: A => Response): Response = {
      getParams(req) match {
        case Some(params) =>
          handler(params)
        case None =>
          Global.badRequest()
      }
    }

    val handler: PartialFunction[HttpRequest, Response] = {
      case req if req.path == jsonPath =>
        withParams(req)(params => Json.format(jsonHandler(params)))

      case req if req.path == path =>
        ContentNegotiation(req) {
          case MimeTypes.`application/json` =>
            withParams(req)(params => Json.format(jsonHandler(params)))
          case _ =>
            withParams(req)(htmlHandler(_))
        }
    }

    handler
  }

  def ContentNegotiation[A](request: HttpRequest)(f: MimeType => A): A = {
    val mime = request.headers.get("Accept")
      .flatMap(_.headOption.flatMap(_.split(",").headOption))
      .map { case MimeTypes(m) => m }
      .getOrElse(MimeTypes.`text/html`)
    f(mime)
  }
}
