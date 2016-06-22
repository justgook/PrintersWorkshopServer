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

class WebSocketIntegrationSpec extends TestKit(ActorSystem("MySpec"))
  with WordSpecLike with Matchers with BeforeAndAfterAll with OneServerPerTest {
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "WebSocket" must {
    "connect to /socket" in {
      val probe = TestProbe()
      WebSocketClient(new URI(s"ws://localhost:$port/thesocket")) {
        case Connected(client) =>
          probe.ref ! "connected"
        //          println("Connection has been established to: " + client.url.toASCIIString)
        //        case Disconnected(client, _) => println("The websocket to " + client.url.toASCIIString + " disconnected.")
        //        case TextMessage(client, message) =>
        //          println("RECV: " + message)
        //          client send ("ECHO: " + message)
      }
      probe.expectMsg("connected")
    }
  }
}
