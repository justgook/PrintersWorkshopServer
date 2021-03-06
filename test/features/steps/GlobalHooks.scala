/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package features.steps

import akka.actor.ActorSystem
import cucumber.api.scala.ScalaDsl
import helpers.PersistenceSuiteTrait
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{Helpers, TestServer}

import scala.concurrent.Await
import scala.concurrent.duration._

trait GlobalHooks extends ScalaDsl {
  lazy val port: Int = Helpers.testServerPort
  implicit var system: ActorSystem = _
  var server: TestServer = _

  Before() { _ =>
    system = ActorSystem("MySpec", PersistenceSuiteTrait.config())
    val app = new GuiceApplicationBuilder()
              .overrides(bind[ActorSystem].toInstance(system))
              .build()
    server = TestServer(port, app)
    server.start()
  }

  After() { _ =>
    system.terminate()
    Await.result(system.whenTerminated, 5 seconds)
    server.stop()
  }
}

