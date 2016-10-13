/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package helpers


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

import scala.concurrent.Future


case class WebSocketAkkaClient(uri: Uri) {

  implicit val system = ActorSystem(getClass.getSimpleName)
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  def open(reader: ActorRef): Future[ActorRef] = {
    val source = Source.actorRef[Message](0, OverflowStrategy.fail)
    val sink = Sink.actorRef[Message](reader, WebSocketAkkaClient.Close)
    val flow = Flow.fromSinkAndSourceMat(sink, source)(Keep.right)

    val (upgradeResponse, writer) = {
      val req = WebSocketRequest(uri)
      Http().singleWebSocketRequest(req, flow)
    }

    upgradeResponse flatMap { upgrade =>
      if (upgrade.response.status != StatusCodes.SwitchingProtocols) {
        Future failed new Exception(s"illegal response status ${upgrade.response.status}")
      }
      else {
        Future successful writer
      }
    }
  }
}


object WebSocketAkkaClient {

  case object Close

}


