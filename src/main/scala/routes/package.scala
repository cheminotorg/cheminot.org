package org.cheminot

import org.joda.time.DateTime

package object router {

  object AsDateTime {
    import org.joda.time.DateTime
    def unapply(s: String): Option[DateTime] =
      scala.util.Try(s.toInt).map(new DateTime(_)).toOption
  }
}
