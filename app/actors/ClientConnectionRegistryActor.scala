package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

//import akka.routing.{BroadcastRoutingLogic, Router}
import play.api.Logger

class ClientConnectionRegistryActor extends Actor with ActorLogging with Subscribers {

  import ClientConnectionRegistryActor._

  override def afterAdd(client: ActorRef): Unit = {
    subscribers.route(ConnectionCountUpdate(subscribers.routees.size), self)
  }

  override def afterTerminated(subscriber: ActorRef): Unit = {
    subscribers.route(ConnectionCountUpdate(subscribers.routees.size), self)

  }

  def receive: Receive = withSubscribers {
    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")

  }
}

object ClientConnectionRegistryActor {
  def props: Props = Props[ClientConnectionRegistryActor]
  case class ConnectionCountUpdate(count: Int)
}

