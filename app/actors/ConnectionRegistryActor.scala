package actors

import akka.actor.{Props, Terminated, ActorRef, Actor}

class ConnectionRegistryActor extends Actor {
  import ConnectionRegistryActor._

  private var connections = Map[ActorRef, ActorRef]()

  override def receive = {
    case Command.Register(ref) =>
      val connection = context watch ref
      connections = connections + (connection -> connection)

    case Terminated(connection) =>
      connections = connections - connection

    case Command.Broadcast(msg) =>
      connections.keys foreach(_ ! msg)

  }
}

object ConnectionRegistryActor {

  sealed trait Command
  object Command {
    case class Register(connection: ActorRef) extends Command
    case class Broadcast(msg: Any) extends Command
  }

  def props: Props = Props[ConnectionRegistryActor]

}