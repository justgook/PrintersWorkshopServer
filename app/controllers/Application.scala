package controllers

/**
  * Created by Roman Potashow on 08.07.2016.
  */

import javax.inject.Inject

import actors.ClientConnectionActor._
import actors.{ClientConnectionActor, ClientConnectionRegistryActor, PrinterRegistryActor}
import akka.actor._
import akka.stream._
import play.api.libs.streams._
import play.api.mvc._
import protocols.ProtocolsRegistryActor
import services.UploadService

class Application @Inject()(implicit system: ActorSystem, m: Materializer) extends Controller {

  val connectionRegistry           = system.actorOf(ClientConnectionRegistryActor.props, "ws-connection-registry")
  val protocolsRegistryActor       = system.actorOf(ProtocolsRegistryActor.props, "protocol-registry")
  val printers                     = system.actorOf(PrinterRegistryActor.props, "printers-registry")
  val uploadService: UploadService = UploadService

  def socket = WebSocket.accept[In, Out] { request =>
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(out, connectionRegistry, protocolsRegistryActor, printers)
    }
  }

  def upload = Action(parse.multipartFormData) { implicit request =>
    val result = uploadService.uploadFile(request)
    Ok(result)
    //    Redirect(routes.Application.index).flashing("message" -> result)
  }
}
