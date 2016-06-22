package services

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

/**
  * Created by Roman Potashow on 20.06.2016.
  */
@Singleton
class HardwareProtocolsSupport @Inject()(clock: Clock, appLifecycle: ApplicationLifecycle) {
  // This code is called when the application starts.
  private val start: Instant = clock.instant
  Logger.debug(s"HardwareProtocolsSupport demo: Starting application at $start.")

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    val stop: Instant = clock.instant
    val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
    Logger.debug(s"HardwareProtocolsSupport demo: Stopping application at ${clock.instant} after ${runningTime}s.")
    Future.successful(())
  }
}



