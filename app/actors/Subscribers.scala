package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}

/**
  * Created by Roman Potashow on 29.06.2016.
  */
trait Subscribers {

  this: Actor with ActorLogging =>
  protected var subscribers = new Router(BroadcastRoutingLogic())

  def withSubscribers(fn: Receive): Receive = receiveExtend orElse fn

  private def receiveExtend: Receive = {
    case Subscribers.Add(subscriber)                                                       =>
      log.debug(s"${self.path.name}(${this.getClass.getName}) got new subscriber")
      context watch subscriber
      subscribers = subscribers.removeRoutee(subscriber)
      subscribers = subscribers.addRoutee(subscriber)
      afterAdd(subscriber)
    case Terminated(subscriber) if subscribers.routees contains ActorRefRoutee(subscriber) =>
      log.debug(s"${self.path.name}(${this.getClass.getName}) delete subscriber")
      subscribers = subscribers.removeRoutee(subscriber)
      afterTerminated(subscriber)
  }

  def afterTerminated(subscriber: ActorRef): Unit = {}

  def afterAdd(subscriber: ActorRef): Unit
}

object Subscribers {
  case class Add(subscriber: ActorRef)
}
