/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

import actors.ClientConnectionRegistryActor.ConnectionCountUpdate
import akka.actor.{Actor, ActorLogging, ActorRef, Props}


class ClientConnectionRegistryActor extends Actor with ActorLogging with Subscribers {

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {
    client ! ConnectionCountUpdate(subscribers.size)
    subscribers.foreach(c => c ! ConnectionCountUpdate(subscribers.size))
    super.afterAdd(client, subscribers)
  }

  override def afterTerminated(client: ActorRef, subscribers: Set[ActorRef]): Unit = {
    subscribers.foreach(c => c ! ConnectionCountUpdate(subscribers.size))
    super.afterTerminated(client, subscribers)
  }

  override def postRestart(reason: Throwable): Unit = {
    println("ClientConnectionRegistryActor: postRestart")
    super.postRestart(reason)
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    println("ClientConnectionRegistryActor: preRestart")
    super.preRestart(reason, message)
  }



  def receive: Receive = subscribersParser(Set.empty)
}

object ClientConnectionRegistryActor {
  def props: Props = Props[ClientConnectionRegistryActor]

  case class ConnectionCountUpdate(count: Int)

}
