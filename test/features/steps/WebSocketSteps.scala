package features.steps

import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import cucumber.api.scala.{EN, ScalaDsl}
import helpers.WebSocketClient
import helpers.WebSocketClient.Messages._

/**
  * Created by Roman Potashow on 22.08.2016.
  */
trait WebSocketSteps extends ScalaDsl with EN {
  var probe: TestProbe = _
  var socket: WebSocketClient = _

  implicit def system: ActorSystem

  def port: Int


  When("""^socket connected to "([^"]*)"$""") { (url: String) =>

    //    system
    probe = TestProbe()
    socket = WebSocketClient(new URI(s"ws://localhost:$port$url"), probe)
    socket.connect()
    probe.expectMsg(Connecting)
    probe.expectMsg(Connected)
    super.After(s => socket.disconnect())
  }

  When("""^initial state received$""") { () =>
    probe.fishForMessage(hint = "set not received") {
      case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
      case _                                                        => false
    }
  }

  When("""^Client-socket send text '([^']*)'$""") { (text: String) =>
    socket.send(s"""$text""")
  }

  When("""^Client-socket send "([^"]*)"$""") { (message: String) =>
    message match {
      case "Ping"  => socket.send("""{"type":"ping"}""")
      case "Reset" => socket.send("""{"type":"reset"}""")
    }
  }


  Then("""^Client-socket got text message "([^"]*)"$""") { (message: String) =>
    probe.fishForMessage(/*max = 100.millis,*/ hint = s"$message not received") {
      case TextMessage(m) => m == message
      case _              => false
    }
  }
  Then("""^Client-socket got "([^"]*)"$""") { (message: String) =>
    message match {
      case "Pong"         =>
        probe.fishForMessage(/*max = 100.millis,*/ hint = "pong not received") {
          case TextMessage("""{"type":"pong"}""") => true
          case _                                  => false
        }
      case "Disconnected" =>
        probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
          case Disconnected(error) => true
          case _                   => false
        }
      case "Fail"         =>
        probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
          case TextMessage(str) if str.startsWith( """{"type":"fail"""") => true
          case _                                                         => false
        }
      case "Set"          =>
        probe.fishForMessage(hint = "set not received") {
          case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
          case _                                                        => false
        }
    }

  }
}
