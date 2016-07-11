package acceptance

import java.net.URI

import acceptance.WebSocketClient.Messages._
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.play._
import play.api.libs.json.{JsObject, _}

/**
  * Created by Roman Potashow on 21.06.2016.
  */
class WebSocketIntegrationSpec
  extends TestKit(ActorSystem("MySpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with OneServerPerTest {

  override def afterAll = TestKit.shutdownActorSystem(system)

  "WebSocket" must {
    "connect to /socket and receive pong " in new TestProbeScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)
      socket.send("""{"type":"ping"}""")
      probe.fishForMessage(/*max = 100.millis,*/ hint = "pong not received") {
        case TextMessage("""{"type":"pong"}""") => true
        case _                                  => false
      }
      socket.disconnect()
      probe.expectMsg(Disconnecting)
      probe.expectMsg(Disconnected(None))
    }

    "get initial state, as `set` command" in new TestProbeScope {
      socket.connect()
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.disconnect()
    }

    "get fail, if send not defined type-command" in new TestProbeScope {
      socket.connect()
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.send("""{"type":"undefined-command"}""")
      probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
        case TextMessage(str) if str.startsWith( """{"type":"fail"""") => true
        case _                                                         => false
      }
      socket.disconnect()

    }

    "close connection, if send not json (or wrong formatted)" in new TestProbeScope {
      socket.connect()
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.send("""not json""")
      probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
        case Disconnected(error) => true
        case _                   => false
      }
      socket.disconnect()
    }

    "get full state, as `set` command after sending `reset`" in new TestProbeScope {
      socket.connect()
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.send("""{"type":"reset"}""")
      probe.fishForMessage(hint = "second set not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.disconnect()
    }

    "have state with connection count as 1" in new TestStateScope {
      socket.connect()
      probe.fishForMessage(hint = "state never had connection count 1") {
        case JsObject(json) =>
          json.get("connections") match {
            case Some(connections) if connections.as[Int] == 1 => true
            case Some(_) | None                                => false
          }
        case _              => false
      }
      socket.disconnect()
    }
    "receive info about connection if create printer with connection configuration" in new TestStateScope {
      socket.connect()
      probe.fishForMessage(hint = "state not got") {
        case JsObject(_) => true
        case _           => false
      }
      socket.send("""{"type":"update","args":[{"op":"add","path":"/printers/-","value":{"settings":{"name":"Test Printer 1","config":{"name":"demoport","properties":{} }}}}]}""")

      probe.fishForMessage(hint = "second set not received") {
        case JsObject(json) => // TODO update it to for comprehension
          json.get("printers") match {
            case Some(p) => (p \ 0 \ "status").isInstanceOf[JsDefined]
            case None    => false
          }
        case _              => false
      }
      socket.disconnect()
    }

    "update printer name" in new TestStateScope {
      socket.connect()
      probe.fishForMessage(hint = "state not got") {
        case JsObject(_) => true
        case _           => false
      }
      socket.send("""{"type":"update","args":[{"op":"add","path":"/printers/-","value":{"settings":{"name":"Test Printer 2","config":{"name":"demoport","properties":{} }}}}]}""")
      probe.fishForMessage(hint = "second set not received") {
        case JsObject(json) => // TODO update it to for comprehension
          json.get("printers") match {
            case Some(p) => (p \ 0 \ "status").isInstanceOf[JsDefined]
            case None    => false
          }
        case _              => false
      }
      socket.send("""{"type":"update","args":[{"op":"replace","path":"/printers/0/settings/name", "value":"New name of printer"}]}""")
      probe.fishForMessage(hint = "second set not received") {
        case JsObject(json) => // TODO update it to for comprehension
          json.get("printers") match {
            case Some(p) => (p \ 0 \ "settings" \ "name").as[String] == "New name of printer"
            case None    => false
          }
        case _              => false
      }
      socket.disconnect()
    }
  }

  trait TestProbeScope {
    val probe  = TestProbe()
    val socket = WebSocketClient(new URI(s"ws://localhost:$port/socket"), probe)
  }

  trait TestStateScope {
    val probe            = TestProbe()
    val stateParserActor = system.actorOf(WebSocketStateActor.props(probe))
    val socket           = WebSocketClient(new URI(s"ws://localhost:$port/socket"), stateParserActor)
  }

}
