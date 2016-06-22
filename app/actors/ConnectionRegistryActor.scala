package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import play.api.Logger

class ConnectionRegistryActor extends Actor {
  import ConnectionRegistryActor._
  Logger.debug("ConnectionRegistryActor created")
  private var connections = Map[ActorRef, ActorRef]()

  override def receive = {

    case Command.Register(ref, client) =>
      val connection = context watch ref
      connections = connections + (connection -> client)
      // TODO it is repeat code find how to wrap it in function
      connections.foreach{ case (key, value) =>
        value ! ClientConnectionActor.Command.ConnectionsCountChange(connections.size)
      }

    case Terminated(connection) =>
      connections = connections - connection
      // TODO it is repeat code find how to wrap it in function
      connections.foreach{ case (key, value) =>
        value ! ClientConnectionActor.Command.ConnectionsCountChange(connections.size)
      }
    //TODO find do i need it ?
//    case Command.Broadcast(msg) =>
//      connections.keys foreach(_ ! msg)
  }
}

object ConnectionRegistryActor {

  sealed trait Command
  object Command {
    case class Register(connection: ActorRef, client: ActorRef) extends Command
//    case class Broadcast(msg: Any) extends Command
  }

  def props: Props = Props[ConnectionRegistryActor]

}