package controllers

import javax.inject._

import actors.{ClientConnectionActor, ConnectionRegistryActor, HardwareProtocolsSupportActor}
import akka.actor._
import akka.stream._
import play.api.libs.streams._
import play.api.mvc._
import actors.ClientConnectionActor._
import play.api.Logger
import actors.ClientConnectionActor.Formats._

//based on https://www.playframework.com/documentation/2.5.x/ScalaWebSockets#WebSockets
class WebSocketController @Inject()(implicit system: ActorSystem, materializer: Materializer) {

  val connectionRegistry = system.actorOf(ConnectionRegistryActor.props, "ws-connection-registry")
  val hardwareProtocolsSupport = system.actorOf(HardwareProtocolsSupportActor.props, "hardware-protocols-support")

  //TODO add IncomingMessage parser
  def socket = WebSocket.accept[In, Out] { request =>
    Logger.info("WebSocket Request")
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(out, connectionRegistry, hardwareProtocolsSupport)
    }
  }

}