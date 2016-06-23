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
    "connect to /socket" in new TestScope {
      socket.connect()
      probe.expectMsg(Connecting)
      probe.expectMsg(Connected)


      socket.send("""{"type":"ping"}""")
      probe.fishForMessage(/*max = 100.millis,*/ hint = "pong not received") {
        case TextMessage("""{"type":"pong"}""") => true
        case _                                  => false
      }
      //      probe.expectMsg(TextMessage("""{"type":"pong"}"""))

      socket.disconnect()
      probe.expectMsg(Disconnecting)
      probe.expectMsg(Disconnected(None))
    }
  }

  trait TestScope {
    val probe  = TestProbe()
    val socket = WebSocketClient(new URI(s"ws://localhost:$port/socket"), probe)
  }

}
