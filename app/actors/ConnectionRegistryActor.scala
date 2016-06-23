package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{BroadcastRoutingLogic, Router}

class ConnectionRegistryActor extends Actor with ActorLogging {


  private var router = Router(BroadcastRoutingLogic())

  override def receive = {

    case client: ActorRef =>
      context watch client
      router = router.removeRoutee(client)
      router = router.addRoutee(client)
      router.route(router.routees.size, self)

    case Terminated(client) =>
      router = router.removeRoutee(client)
      router.route(router.routees.size, self)


  }
}

object ConnectionRegistryActor {

  def props: Props = Props[ConnectionRegistryActor]

}