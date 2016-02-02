package org.cheminot

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

package object api {

  def formatDateTime(dateTime: DateTime): String =
    ISODateTimeFormat.dateTime.print(dateTime)
}
