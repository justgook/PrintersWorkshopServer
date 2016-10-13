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


class ProtocolsRegistryActor extends Actor with ActorLogging with Subscribers {

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {
    client ! SettingsList(protocols.settings) // TODO update it to real Connection Check / update not just static array
  }

  def receive: Receive = subscribersParser(Set.empty)
}

object ProtocolsRegistryActor {
  def props: Props = Props[ProtocolsRegistryActor]
}
