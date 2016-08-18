package actors

/**
  * Created by Roman Potashow on 11.08.2016.
  */

import actors.PrinterConnectionRegistryActor.DirectConnection
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import protocols.Connection.ConsoleInput

import scala.concurrent.Await
import scala.concurrent.duration._


class TerminalWebSocketActor(out: ActorRef, name: String, printersConnections: ActorRef) extends Actor with ActorLogging {

  implicit val timeout = Timeout(5 seconds)
  val future = printersConnections ? DirectConnection(name)
  val result = Await.result(future, timeout.duration)
  result match {
    case connection: ActorRef =>
      log.info(s"$connection ! Subscribers.Add($self)")
      connection ! Subscribers.Add(self)
      context become opened(connection)
    case None                 =>
      log.warning(s"Printer '$name' not found")
      self ! PoisonPill
    case msg                  => log.error(s"got unexpected $msg")
  }

  def receive = opened(Actor.noSender)

  def opened(connection: ActorRef): Receive = {
    case msg: String if sender == connection => out ! msg
    case msg: String                         => connection ! ConsoleInput(msg)
    case msg                                 => log.error(s"got unexpected $msg, $sender")
  }

  override def postStop() = {
    context.parent ! PoisonPill // https://github.com/playframework/playframework/issues/6408 - remove me when it is closed
  }
}

object TerminalWebSocketActor {
  def props(out: ActorRef, name: String, printersConnections: ActorRef) = Props(new TerminalWebSocketActor(out, name, printersConnections))
}