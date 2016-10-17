/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package protocols


import actors.Subscribers
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import protocols.Protocol.SettingsList

/**
  * Created by Roman Potashow on 01.07.2016.
  */
//import scala.concurrent.duration._

class ProtocolsRegistryActor extends Actor with ActorLogging with Subscribers {

  //  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.second) {
  //    case _: ArithmeticException      => Resume
  //    case _: NullPointerException     => Restart
  //    case _: IllegalArgumentException => Stop
  //    case _: Exception                => Escalate
  //  }

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {
    client ! SettingsList(protocols.settings) // TODO update it to real Connection Check / update not just static array
    super.afterAdd(client, subscribers)
  }

  def receive: Receive = subscribersParser(Set.empty)

  override def preStart {
    println("ProtocolsRegistryActor: preStart")
  }

  override def postStop {
    println("ProtocolsRegistryActor: postStop")
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    println(s"ProtocolsRegistryActor: preRestart - $message  ")
    message match {
      case Some(m) => self ! m
      case None    =>
    }
    super.preRestart(reason, message)
  }
}

object ProtocolsRegistryActor {
  def props: Props = Props[ProtocolsRegistryActor]
}
