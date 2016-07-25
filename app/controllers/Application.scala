package controllers

/**
  * Created by Roman Potashow on 08.07.2016.
  */

import javax.inject._

import actors.ClientConnectionActor
import actors.ClientConnectionActor._
import akka.actor._
import akka.stream._
import play.api.libs.streams._
import play.api.mvc._
import services.UploadService

@Singleton
class Application @Inject()
(@Named("ws-connection-registry") connectionRegistry: ActorRef)
(@Named("protocol-registry") protocolsRegistryActor: ActorRef)
(@Named("printers-registry") printers: ActorRef)
(implicit system: ActorSystem, m: Materializer) extends Controller {

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
