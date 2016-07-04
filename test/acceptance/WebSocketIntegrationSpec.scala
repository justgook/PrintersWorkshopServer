package acceptance

import java.net.URI

import acceptance.WebSocketClient.Messages._
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.play._


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
    "connect to /socket and receive pong " in new TestScope {
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

    "get initial state, as `set` command" in new TestScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.disconnect()
    }

    "get fail, if send not defined type-command" in new TestScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)
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

    "close connection, if send not json (or wrong formatted)" in new TestScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)

      socket.send("""not json """)
      probe.fishForMessage(/*max = 100.millis,*/ hint = "fail not received") {
        case Disconnected(error) => true
        case _                   => false
      }
      socket.disconnect()
    }

    "get full state, as `set` command after sending `reset`" in new TestScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.send("""{"type":"reset"}""")
      probe.fishForMessage(hint = "pong not received") {
        case TextMessage(str) if str.startsWith( """{"type":"set"""") => true
        case _                                                        => false
      }
      socket.disconnect()
    }

  }

  trait TestScope {
    val probe  = TestProbe()
    val socket = WebSocketClient(new URI(s"ws://localhost:$port/socket"), probe)
  }

}
