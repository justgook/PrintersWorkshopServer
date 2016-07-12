package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}


class ClientConnectionRegistryActor extends Actor with ActorLogging with Subscribers {

  import ClientConnectionRegistryActor._

  override def afterAdd(client: ActorRef): Unit = {
    subscribers.route(ConnectionCountUpdate(subscribers.routees.size), self)
  }

  override def afterTerminated(subscriber: ActorRef): Unit = {
    subscribers.route(ConnectionCountUpdate(subscribers.routees.size), self)

  }

  def receive: Receive = withSubscribers {
    case msg => log.warning(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")

  }
}

object ClientConnectionRegistryActor {
  def props: Props = Props[ClientConnectionRegistryActor]
  case class ConnectionCountUpdate(count: Int)
}

