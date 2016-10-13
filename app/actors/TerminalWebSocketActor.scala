/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

/**
  * Created by Roman Potashow on 11.08.2016.
  */

import actors.PrinterConnectionRegistryActor.DirectConnection
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import protocols.Connection.ConsoleInput

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class TerminalWebSocketActor(out: ActorRef, name: String, printersConnections: ActorRef) extends Actor with ActorLogging {

  implicit val timeout = Timeout(5 seconds)
  val future: Future[Any] = printersConnections ? DirectConnection(name)
  val result: Any = Await.result(future, timeout.duration)
  result match {
    case connection: ActorRef =>
      log.info(s"$connection ! Subscribers.Add($self)")
      connection ! Subscribers2.Add(self)
      context become opened(connection)
    case None                 =>
      log.warning(s"Printer '$name' not found")
      self ! PoisonPill
    case msg                  => log.error(s"got unexpected $msg")
  }

  def receive: Receive = opened(Actor.noSender)

  def opened(connection: ActorRef): Receive = {
    case msg: String if sender == connection => out ! msg
    case msg: String                         => connection ! ConsoleInput(msg)
    case msg                                 => log.error(s"got unexpected $msg, $sender")
  }

  override def postStop(): Unit = {
    context.parent ! PoisonPill // https://github.com/playframework/playframework/issues/6408 - remove me when it is closed
  }
}

object TerminalWebSocketActor {
  def props(out: ActorRef, name: String, printersConnections: ActorRef) = Props(new TerminalWebSocketActor(out, name, printersConnections))
}
