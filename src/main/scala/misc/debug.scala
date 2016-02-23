package org.cheminot.web.misc

object Debug {

  def measure[A](label: String)(f: => A): A = {
    val start = System.currentTimeMillis
    val x = f
    println(s"[$label]> ${System.currentTimeMillis - start} ms")
    x
  }
}
