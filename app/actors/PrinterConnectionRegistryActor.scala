/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

import actors.Subscribers2.{AfterAdd, AfterTerminated}
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import protocols.Connection.Status
import protocols.{Configuration, StatusText}

class PrinterConnectionRegistryActor
  extends Actor with ActorLogging with Subscribers2 {
  log.info("PrinterConnectionRegistryActor Created")

  import actors.PrinterConnectionRegistryActor._

  def receive: Receive = subscribersParser(Set.empty).orElse[Any, Unit](receive(Actor.noSender, Map.empty, Set.empty))

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {}

  override def afterTerminated(subscriber: ActorRef, subscribers: Set[ActorRef]): Unit = {}

  def receive(printersSettings: ActorRef, connections: Map[ActorRef, (String, Status)], subscribers: Set[ActorRef]): Receive = {
    case AfterTerminated(_, newSubscribers)                        =>
      context become subscribersParser(newSubscribers).orElse[Any, Unit](receive(printersSettings, connections, newSubscribers))
    case AfterAdd(newSubscriber, newSubscribers)                   =>
      newSubscriber ! PrinterConnections(connections.values.toMap[String, Status])
      context become subscribersParser(newSubscribers).orElse[Any, Unit](receive(printersSettings, connections, newSubscribers))
    case DirectConnection(name: String)                            =>
      connections.find((p) => p._2._1 == name) match {
        case Some(c) => sender() ! c._1
        case None    => sender() ! None //log.warning(s"no connection for printer '$name'")
      }
    case (name: String, config: Configuration)                     =>
      val ref = protocols.connect(config, context)
      context watch ref
      context become subscribersParser(subscribers).orElse[Any, Unit](receive(sender(), connections + (ref -> (name, Status())), subscribers))
      log.info(s"got request to connect $name with settings $config")
    case (name: String, PoisonPill)                                =>
      connections.find((p) => p._2._1 == name) match {
        case Some(c) => c._1 ! PoisonPill
        case None    => log.warning(s"no connection for printer '$name'")
      }
    case status: Status                                            =>
      val ref = sender()
      connections.get(ref) match {
        case Some(tuple) =>
          log.info(s"update for $tuple")
          printersSettings ! (tuple._1, StatusText.Connected)
          context become subscribersParser(subscribers).orElse[Any, Unit](receive(printersSettings, connections + (ref -> (tuple._1, status)), subscribers))
        case None        => log.warning(s"got status update from deleted connection")
      }
      subscribers.foreach(c => c ! PrinterConnections(connections.values.toMap[String, Status]))
    case Terminated(connection) if connections contains connection =>
      log.info(s"removing connection $connection")
      val newConnection = connections - connection
      subscribers.foreach(c => c ! PrinterConnections(newConnection.values.toMap[String, Status]))
      context become subscribersParser(subscribers).orElse[Any, Unit](receive(printersSettings, newConnection, subscribers))
    case msg => log.warning(s"got $msg")
  }
}

object PrinterConnectionRegistryActor {
  def props: Props = Props[PrinterConnectionRegistryActor]

  case class PrinterConnections(list: Map[String, Status])

  case class DirectConnection(name: String)

}
