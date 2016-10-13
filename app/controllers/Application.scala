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
import play.api.libs.Files.TemporaryFile
import play.api.libs.streams._
import play.api.libs.ws.WSClient
import play.api.mvc._
import protocols.ProtocolsRegistryActor
import services.UploadService

import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class Application @Inject()(ws: WSClient, configuration: play.api.Configuration)(implicit system: ActorSystem, m: Materializer) extends Controller {

  val connectionRegistry: ActorRef = system.actorOf(ClientConnectionRegistryActor.props, "connection-registry")
  val protocolsRegistryActor: ActorRef = system.actorOf(ProtocolsRegistryActor.props, "protocol-registry")
  val printersConnections: ActorRef = system.actorOf(PrinterConnectionRegistryActor.props, "printers-connections-registry")
  val printersSettings: ActorRef = system.actorOf(PrinterSettingsRegistryActor.props(printersConnections), "printers-settings-registry")

  val fileRegistry: ActorRef = system.actorOf(FileRegistryActor.props(configuration.underlying.getString("printerWorkshop.fileDir")), "file-registry")

  val uploadService: UploadService = UploadService

  def proxy(url: String): Action[AnyContent] = Action.async { request =>
    ws.url(s"http://$url").get().map(resp => Ok(resp.body).as("text/html"))
  }

  def proxyIndexJs(url: String): Action[AnyContent] = Action.async { request =>
    ws.url(s"http://localhost:8000/index.js").get().map(resp => Ok(resp.body))
  }

  def socket: WebSocket = WebSocket.accept[In, Out] { request =>
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

  def terminalSocket(name: String): WebSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => TerminalWebSocketActor.props(out, name, printersConnections))
  }


  def upload: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    val result = uploadService.uploadFile(request)
    Ok(result)
    //    Redirect(routes.Application.index).flashing("message" -> result)
  }
}
