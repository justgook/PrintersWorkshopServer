package acceptance

import java.net.URI

import acceptance.helpers.WebSocketClient.Messages._
import acceptance.helpers.{StateObserver, WebSocketClient}
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.play._
import play.api.libs.json._

import scala.language.reflectiveCalls

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
      probe.fishForMessage(hint = "set not received") {
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

    "get fail message if try send update with wrong revision number" in new TestProbeScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)
      socket.send("""{"type":"update", "revision": 9999, "args":[]}""")
      probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
        case TextMessage(str) if str.startsWith( """{"type":"fail"""") => true
        case _                                                         => false
      }
      socket.disconnect()
    }

    "get fail message if try send update inapplicable to state" in new TestProbeScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)
      socket.send("""{"type":"update", "revision": 2, "args":[{"op":"replace","path":"/printers/1","value":{"name":"Test Printer", "settings":{"name":"demoport","properties":{} }}}]}""")
      probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
        case TextMessage(str) if str.startsWith( """{"type":"fail"""") => true
        case _                                                         => false
      }
      socket.disconnect()
    }

    "increment state revision each update" in new ClientState {
      socket.connect()

      stateProbe.expectInitialState()

      socket.send("""{"type":"update", "revision": 2, "args":[{"op":"add","path":"/printers/-","value":{"name":"Test Printer", "settings":{"name":"demoport","properties":{} }}}]}""")
      stateProbe.fishForMessage(hint = "state not got") {
        case (state: JsObject, rev) => rev == 2
        case _                      => false
      }
      socket.disconnect()
    }

    "get update connection count" in new ClientState {
      val probe2  = TestProbe()
      val socket2 = WebSocketClient(new URI(s"ws://localhost:$port/socket"), probe2)
      socket2.connect()
      socket.connect()

      stateProbe.expectConnectionCount(2)

      socket2.disconnect()
      socket.disconnect()
    }

    "receive info about printer status if create printer with connection configuration" in new ClientState {
      val probe2 = TestProbe()

      socket.connect()
      stateProbe.expectInitialState()

      sendUpdate(Json.parse("""[{"op":"add","path":"/printers/-","value":{"name":"Test Printer", "settings":{"name":"demoport","properties":{} }}}]""").as[JsArray])
      stateProbe.fishForMessage(hint = "printers status not got") {
        case (state: JsObject, rev) => (state \ "printers" \ 0 \ "status").isInstanceOf[JsDefined]
        case _                      => false
      }
      socket.disconnect()
    }
    //    "update printer name" in new ClientState {
    //      socket.connect()
    //      stateProbe.expectInitialState()
    //      withPrinter("1")
    //      sendUpdate(Json.parse("""[{"op":"replace","path":"/printers/0/name","value":"New Name of Test Printer"}]""").as[JsArray])
    //      stateProbe.fishForMessage(hint = "State not updated") {
    //        case (state: JsObject, rev) => true
    //        case _                      => false
    //      }
    //      socket.send("""{"type":"reset"}""")
    //      stateProbe.fishForMessage(hint = "printers status not got") {
    //        case (state: JsObject, rev) => (for {name <- (state \ "printers" \ 0 \ "name").asOpt[String] if name == "New Name of Test Printer"} yield true) getOrElse false
    //        case _                      => false
    //      }
    //      socket.disconnect()
    //    }
    //    "delete printer" in new ClientState {
    //      socket.connect()
    //      stateProbe.expectInitialState()
    //      withPrinter("1")
    //      deletePrinter(0)
    //      socket.disconnect()
    //    }

    "set status text as ready to print if add file for printing" in new ClientState {
      socket.connect()
      stateProbe.expectInitialState()
      withPrinter("1")
      sendUpdate(Json.parse("""[{"op":"replace","path":"/printers/0/status/file","value":"testFile.gcode"}]""").as[JsArray])

      stateProbe.fishForMessage(hint = "printer 0 status never updated") {
        case (state: JsObject, rev) => (for {statusText <- (state \ "printers" \ 0 \ "status" \ "text").asOpt[String] if statusText == "ready"} yield true) getOrElse false
        case _                      => false
      }
      socket.disconnect()
    }
  }

  trait TestProbeScope {
    val probe  = TestProbe()
    val socket = WebSocketClient(new URI(s"ws://localhost:$port/socket"), probe)
  }

  trait ClientState {
    //TODO update it to proxy
    val stateProbe = new TestProbe(system) {
      def expectInitialState() = {
        fishForMessage(hint = "state not got") {
          case (state: JsObject, rev) => rev == 1
          case _                      => false
        }
      }


      def expectConnectionCount(n: Int) = {
        fishForMessage(hint = s"state never had connection count $n") {
          case (state: JsObject, rev) => (for {connections <- (state \ "connections").asOpt[Int] if connections == n} yield true) getOrElse false
          case _                      => false
        }
      }
    }

    val client = system.actorOf(StateObserver.props(stateProbe))
    val socket = WebSocketClient(new URI(s"ws://localhost:$port/socket"), client)

    def withPrinter(name: String = "Test Printer"): Unit = {
      sendUpdate(Json.parse(s"""[{"op":"add","path":"/printers/-","value":{"name":"$name", "settings":{"name":"demoport","properties":{} }}}]""").as[JsArray])
      stateProbe.fishForMessage(hint = "printers status not got") {
        case (state: JsObject, rev) => (state \ "printers" \ 0 \ "status").isInstanceOf[JsDefined] // TODO update to searching by name
        case _                      => false
      }
    }

    def sendUpdate(args: JsArray) = {
      client ! (args, socket)
    }

    //TODO update to indexing by name, no by index
    def deletePrinter(index: Int = 0) = {
      sendUpdate(Json.parse(s"""[{"op":"remove","path":"/printers/$index"}]""").as[JsArray])
      stateProbe.fishForMessage(hint = "printers array not empty") {
        //TODO add validtion that printer is gone, not all printers are deleted
        case (state: JsObject, rev) => (for {size <- (state \ "printers").asOpt[JsArray].map(_.value.size) if size == 0} yield true) getOrElse false //.isInstanceOf[JsDefined]
        case _                      => false
      }
    }
  }

}
