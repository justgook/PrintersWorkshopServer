/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package features.steps

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.TextMessage
import akka.testkit.TestProbe
import cucumber.api.scala.{EN, ScalaDsl}
import helpers.WebSocketAkkaClient

import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * Created by Roman Potashow on 22.08.2016.
  */

trait WebSocketSteps extends ScalaDsl with EN {
  var probe: TestProbe = _
  var socket: WebSocketAkkaClient = _
  var writer: ActorRef = _

  implicit def system: ActorSystem
  def port: Int

  When("""^socket connected to "([^"]*)"$""") { (url: String) =>
    probe = TestProbe()
    val socketConnection = WebSocketAkkaClient(s"ws://localhost:$port$url")
    writer = Await.result(socketConnection.open(probe.ref), 5.second)
  }

  When("""^initial state received$""") { () =>
    probe.fishForMessage(hint = "set not received") {
      case TextMessage.Strict(str) if str.startsWith( """{"type":"set"""") => true
      case _                                                               => false
    }
  }

  When("""^Client-socket send text '([^']*)'$""") { (text: String) =>
    writer ! TextMessage(s"""$text""")
  }

  When("""^Client-socket send "([^"]*)"$""") { (message: String) =>
    message match {
      case "Ping"  => writer ! TextMessage("""{"type":"ping"}""")
      case "Reset" => writer ! TextMessage("""{"type":"reset"}""")
    }
  }


  Then("""^Client-socket got text message "([^"]*)"$""") { (message: String) =>
    probe.fishForMessage(/*max = 100.millis,*/ hint = s"$message not received") {
      case TextMessage.Strict(m) => m == message
      case _                     => false
    }
  }
  Then("""^Client-socket got "([^"]*)"$""") { (message: String) =>
    message match {
      case "Pong"         =>
        probe.fishForMessage(/*max = 100.millis,*/ hint = "pong not received") {
          case TextMessage.Strict("""{"type":"pong"}""") => true
          case _                                         => false
        }
      case "Disconnected" =>
        probe.fishForMessage(/*max = 100.millis,*/ hint = "Disconnected not received") {
          case Failure(_) => true
          case t          => false
        }
      case "Fail"         =>
        probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
          case TextMessage.Strict(str) if str.startsWith( """{"type":"fail"""") => true
          case _                                                                => false
        }
      case "Set"          =>
        probe.fishForMessage(hint = "set not received") {
          case TextMessage.Strict(str) if str.startsWith( """{"type":"set"""") => true
          case _                                                               => false
        }
    }

  }
}
