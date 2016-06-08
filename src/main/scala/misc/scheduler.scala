package org.cheminot.web.misc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

object Scheduler {

  def schedule(delay: FiniteDuration, period: FiniteDuration, f: => Unit): ScheduledExecutor = {
    val executor = ScheduledExecutor(1)
    executor.delayExecution(f)(period).foreach { _ =>
      executor.schedule(f)(period)
    }
    executor
  }
}
