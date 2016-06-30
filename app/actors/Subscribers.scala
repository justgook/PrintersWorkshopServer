package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}
import play.api.Logger

/**
  * Created by Roman Potashow on 29.06.2016.
  */
trait Subscribers {

  this: Actor with ActorLogging =>
  protected var subscribers = new Router(BroadcastRoutingLogic())

  def withSubscribers(fn: Receive): Receive = receiveExtend orElse fn

  private def receiveExtend: Receive = {
    case Subscribers.Add(subscriber)                                                       =>
      Logger.debug(s"${self.path.name}(${this.getClass.getName}) got new subscriber")
      context watch subscriber
      subscribers = subscribers.removeRoutee(subscriber)
      subscribers = subscribers.addRoutee(subscriber)
      afterAdd(subscriber)
    case Terminated(subscriber) if subscribers.routees contains ActorRefRoutee(subscriber) =>
      Logger.debug(s"${self.path.name}(${this.getClass.getName}) delete subscriber")
      subscribers = subscribers.removeRoutee(subscriber)
      afterTerminated(subscriber)
  }

  def afterAdd(subscriber: ActorRef): Unit = {}

  def afterTerminated(subscriber: ActorRef): Unit = {}
}


object Subscribers {
  case class Add(subscriber: ActorRef)
}
