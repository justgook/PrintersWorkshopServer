package controllers

import javax.inject._

import actors.{ConnectionRegistryActor, MyWebSocketActor}
import akka.actor._
import akka.stream._
import play.api.libs.json.JsValue
import play.api.libs.streams._
import play.api.mvc._


//based on https://www.playframework.com/documentation/2.5.x/ScalaWebSockets#WebSockets
class WebSocketController @Inject()(implicit system: ActorSystem, materializer: Materializer) {

  val connectionRegistry = system.actorOf(ConnectionRegistryActor.props, "ws-connection-registry")

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow actorRef { out =>
      connectionRegistry ! ConnectionRegistryActor.Command.Register(out)
      MyWebSocketActor.props(out, connectionRegistry)
    }
  }

}