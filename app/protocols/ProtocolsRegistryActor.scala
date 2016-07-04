package protocols


import actors.Subscribers
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.Logger
import protocols.Protocol.SettingsUpdate

/**
  * Created by Roman Potashow on 01.07.2016.
  */


class ProtocolsRegistryActor extends Actor with ActorLogging with Subscribers {

  override def afterAdd(client: ActorRef): Unit = {
    client ! SettingsUpdate(protocols.settings) // TODO update it to real Connection Check / update not just static array
  }

  override def afterTerminated(subscriber: ActorRef): Unit = {}

  def receive = withSubscribers {
    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}

object ProtocolsRegistryActor {
  def props: Props = Props[ProtocolsRegistryActor]
}
