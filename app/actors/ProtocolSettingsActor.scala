package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.{BroadcastRoutingLogic, Router}
import play.api.Logger
import protocols.{Settings => ProtocolSettings}
/**
  * Created by Roman Potashow on 20.06.2016.
  */

//TODO rename to PrinterTransportSetting And move it Under [[actors.protocol]] package
class ProtocolSettingsActor extends Actor with ActorLogging with Subscribers {

  import ProtocolSettingsActor._
  var router = Router(BroadcastRoutingLogic())

  override def afterAdd(client: ActorRef): Unit = {
    client ! ProtocolSettingsUpdate(protocols.settings)
  }

  override def afterTerminated(subscriber: ActorRef): Unit = {}

  def receive = withSubscribers {
    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}


object ProtocolSettingsActor {

  def props: Props = Props[ProtocolSettingsActor]
  sealed trait Message
  case class ProtocolSettingsUpdate(list: List[ProtocolSettings]) extends Message

}
