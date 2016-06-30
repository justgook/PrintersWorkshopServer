package controllers

import javax.inject._

import actors.ClientConnectionActor.Formats._
import actors.ClientConnectionActor._
import actors.{ClientConnectionActor, ClientConnectionRegistryActor, PrinterRegistryActor, ProtocolSettingsActor}
import akka.actor._
import akka.stream._
import play.api.libs.streams._
import play.api.mvc._


class WebSocketController @Inject()(implicit system: ActorSystem, m: Materializer) {
  //TODO move those to some service - they must start with application not first WS connection
  val connectionRegistry = system.actorOf(ClientConnectionRegistryActor.props, "ws-connection-registry")
  val protocolSettings   = system.actorOf(ProtocolSettingsActor.props, "protocol-settings")
  val printers           = system.actorOf(PrinterRegistryActor.props, "printers-registry")

  //TODO add IncomingMessage parser
  def socket = WebSocket.accept[In, Out] { request =>
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(out, connectionRegistry, protocolSettings, printers)
    }
  }

}
