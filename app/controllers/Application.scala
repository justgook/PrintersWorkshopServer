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
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.UploadService

import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class Application @Inject()(
                             ws: WSClient,
                             @Named("ws-connection-registry") connectionRegistry: ActorRef,
                             @Named("protocol-registry") protocolsRegistryActor: ActorRef,
                             @Named("printers-registry") printersSettings: ActorRef,
                             @Named("printers-connections") printersConnections: ActorRef)
                           (implicit system: ActorSystem, m: Materializer) extends Controller {

  val uploadService: UploadService = UploadService

  def proxy(url: String) = Action.async { request =>
    ws.url(s"http://$url").get().map(resp => Ok(resp.body).as("text/html"))
  }

  def proxyIndexJs(url: String) = Action.async { request =>
    ws.url(s"http://localhost:8000/index.js").get().map(resp => Ok(resp.body))
  }

  def socket = WebSocket.accept[In, Out] { request =>
    ActorFlow actorRef { out =>
      ClientConnectionActor.props(out, connectionRegistry, protocolsRegistryActor, printersSettings, printersConnections)
    }
  }

  def upload = Action(parse.multipartFormData) { implicit request =>
    val result = uploadService.uploadFile(request)
    Ok(result)
    //    Redirect(routes.Application.index).flashing("message" -> result)
  }
}
