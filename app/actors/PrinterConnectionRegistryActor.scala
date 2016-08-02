package actors

import javax.inject.Named

import actors.PrinterConnectionRegistryActor.PrinterConnections
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.google.inject.Inject
import protocols.Connection.{Configuration, Status}
import protocols.StatusText

/**
  * Created by Roman Potashow on 02.08.2016.
  */
class PrinterConnectionRegistryActor @Inject()
(@Named("printers-registry") printersSettings: ActorRef)
  extends Actor with ActorLogging with Subscribers {
  log.info("PrinterConnectionRegistryActor Created")
  private var connections: Map[ActorRef, (String, Status)] = Map.empty

  override def afterAdd(client: ActorRef) = client ! PrinterConnections(connections.values.toMap[String, Status])

  def receive = withSubscribers {
    case (name: String, config: Configuration) =>
      val ref = protocols.connect(config, context)
      context watch ref
      connections += ref -> (name, Status())

      log.info(s"got request to connect $name with settings $config")
    case status: Status                        =>
      val ref = sender()
      connections.get(ref) match {
        case Some(tuple) =>
          log.info(s"update for $tuple")
          connections += ref -> (tuple._1, status)
          printersSettings ! (tuple._1, StatusText.Connected)
        case None        => log.warning(s"got status update from deleted connection")
      }
      subscribers.route(PrinterConnections(connections.values.toMap[String, Status]), self)
    case msg                                   => log.warning(s"got $msg")
  }
}
object PrinterConnectionRegistryActor {
  def props: Props = Props[PrinterConnectionRegistryActor]
  case class PrinterConnections(list: Map[String, Status])
}
