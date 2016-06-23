package controllers

import javax.inject._

import actors.ClientConnectionActor.Formats._
import actors.ClientConnectionActor._
import actors.{ClientConnectionActor, ConnectionRegistryActor, HardwareProtocolsSupportActor}
import akka.actor._
import akka.stream._
import play.api.libs.streams._
import play.api.mvc._

//based on https://www.playframework.com/documentation/2.5.x/ScalaWebSockets#WebSockets
class WebSocketController @Inject()(implicit system: ActorSystem, m: Materializer) {

  val connectionRegistry       = system.actorOf(ConnectionRegistryActor.props, "ws-connection-registry")
  val hardwareProtocolsSupport = system.actorOf(HardwareProtocolsSupportActor.props, "hardware-protocols-support")

  //TODO add IncomingMessage parser
  def socket = WebSocket.accept[In, Out] { request =>
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(out, connectionRegistry, hardwareProtocolsSupport)
    }
  }

}