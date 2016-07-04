package controllers

import javax.inject._

import actors.ClientConnectionActor.Formats._
import actors.ClientConnectionActor._
import actors.{ClientConnectionActor, ClientConnectionRegistryActor, PrinterRegistryActor}
import akka.actor._
import akka.stream._
import play.api.libs.streams._
import play.api.mvc._
import protocols.ProtocolsRegistryActor


class WebSocketController @Inject()(implicit system: ActorSystem, m: Materializer) {
  //TODO move those to some service - they must start with application not first WS connection
  val connectionRegistry     = system.actorOf(ClientConnectionRegistryActor.props, "ws-connection-registry")
  val protocolsRegistryActor = system.actorOf(ProtocolsRegistryActor.props, "protocol-registry")
  val printers               = system.actorOf(PrinterRegistryActor.props, "printers-registry")

  def socket = WebSocket.accept[In, Out] { request =>
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(out, connectionRegistry, protocolsRegistryActor, printers)
    }
  }

}
