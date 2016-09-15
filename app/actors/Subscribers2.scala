/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

import actors.Subscribers2.{AfterAdd, AfterTerminated}
import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

/**
  * Created by Roman Potashow on 29.06.2016.
  */
trait Subscribers2 {
  this: Actor with ActorLogging =>
  //  protected var subscribers

  //  def withSubscribers(subscribers: Set[ActorRef])(fn: Receive): Receive = receiveExtend(subscribers) orElse fn

  def subscribersParser(subscribers: Set[ActorRef]): Receive = {
    case Subscribers.Add(subscriber) =>
      log.debug("{}({}) got new subscriber", self.path.name, this.getClass.getName)
      context watch subscriber
      val newSubscribers = subscribers + subscriber
      self ! AfterAdd(subscriber, newSubscribers)
      afterAdd(subscriber, newSubscribers)

    case Terminated(subscriber) if subscribers contains subscriber =>
      log.debug(s"{}({}) delete subscriber", self.path.name, this.getClass.getName)
      val newSubscribers = subscribers - subscriber
      self ! AfterTerminated(subscriber, newSubscribers)
      afterTerminated(subscriber, newSubscribers)
  }

  def afterTerminated(subscriber: ActorRef, subscribers: Set[ActorRef]): Unit = context.become(subscribersParser(subscribers))

  def afterAdd(subscriber: ActorRef, subscribers: Set[ActorRef]): Unit = context.become(subscribersParser(subscribers))
}

object Subscribers2 {

  case class Add(subscriber: ActorRef)

  case class AfterAdd(subscriber: ActorRef, subscribers: Set[ActorRef])

  case class AfterTerminated(subscriber: ActorRef, subscribers: Set[ActorRef])

}
