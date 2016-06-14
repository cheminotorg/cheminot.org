package org.cheminot.web

import org.cheminot.misc
import org.slf4j.LoggerFactory

object Logger extends misc.log.LoggerLike {

  val logger = LoggerFactory.getLogger("cheminot.web")
}
