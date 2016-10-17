/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package controllers

/**
  * Created by Roman Potashow on 08.07.2016.
  */


import actors.ClientConnectionActor._
import actors._
import akka.actor._
import akka.stream._
import com.google.inject.{Inject, Singleton}
import play.api.libs.streams._
import play.api.mvc._
import protocols.ProtocolsRegistryActor

import scala.concurrent.duration._

@Singleton
class Application @Inject()(configuration: play.api.Configuration)(implicit system: ActorSystem, m: Materializer) extends Controller {

  val immutableSupervisorFactory = ImmutableSupervisorFactory(
    minBackoff =.5.seconds,
    maxBackoff = 1.seconds,
    timeout = 5.seconds,
    awaitResult = 1.seconds
  )

  val protocolsRegistryActor: ActorRef = immutableSupervisorFactory.actorOf(ProtocolsRegistryActor.props, "protocol-registry")
  val connectionRegistry: ActorRef = immutableSupervisorFactory.actorOf(ClientConnectionRegistryActor.props, "connection-registry")
  val printersConnections: ActorRef = immutableSupervisorFactory.actorOf(PrinterConnectionRegistryActor.props, "printers-connections-registry")
  val printersSettings: ActorRef = immutableSupervisorFactory.actorOf(PrinterSettingsRegistryActor.props(printersConnections), "printers-settings-registry")
  val fileRegistry: ActorRef = immutableSupervisorFactory.actorOf(FileRegistryActor.props(configuration.underlying.getString("printerWorkshop.fileDir")), "file-registry")


  def socket: WebSocket = WebSocket.accept[In, Out] { _ =>
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(
        out,
        connectionRegistry,
        protocolsRegistryActor,
        printersSettings,
        printersConnections,
        fileRegistry
      )
    }
  }

  def terminalSocket(name: String): WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef(out => TerminalWebSocketActor.props(out, name, printersConnections))
  }
}
