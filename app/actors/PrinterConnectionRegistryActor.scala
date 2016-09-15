/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import protocols.Connection.Status
import protocols.{Configuration, StatusText}

class PrinterConnectionRegistryActor
  extends Actor with ActorLogging with Subscribers {

  import actors.PrinterConnectionRegistryActor._
  log.info("PrinterConnectionRegistryActor Created")
  private var connections: Map[ActorRef, (String, Status)] = Map.empty

  override def afterAdd(client: ActorRef) = client ! PrinterConnections(connections.values.toMap[String, Status])

  def receive: Receive = receive(Actor.noSender)

  def receive(printersSettings: ActorRef): Receive = withSubscribers {
    case DirectConnection(name: String) =>
      connections.find((p) => p._2._1 == name) match {
        case Some(c) => sender() ! c._1
        case None    => sender() ! None //log.warning(s"no connection for printer '$name'")
      }

    case (name: String, config: Configuration) =>
      context.become(receive(sender()))
      val ref = protocols.connect(config, context)
      context watch ref
      connections += ref -> (name, Status())
      log.info(s"got request to connect $name with settings $config")
    case (name: String, PoisonPill)            =>
      connections.find((p) => p._2._1 == name) match {
        case Some(c) => c._1 ! PoisonPill
        case None    => log.warning(s"no connection for printer '$name'")
      }
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

    case Terminated(connection) if connections contains connection =>
      log.info(s"removing connection $connection")
      connections -= connection
      subscribers.route(PrinterConnections(connections.values.toMap[String, Status]), self)
    case msg                                   => log.warning(s"got $msg")
  }
}
object PrinterConnectionRegistryActor {
  def props = Props[PrinterConnectionRegistryActor]
  case class PrinterConnections(list: Map[String, Status])
  case class DirectConnection(name: String)
}
