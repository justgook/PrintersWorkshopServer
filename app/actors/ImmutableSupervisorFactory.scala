/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.BackoffSupervisor.{CurrentChild, GetCurrentChild}
import akka.pattern.{Backoff, BackoffSupervisor, ask}
import akka.util.Timeout

import scala.concurrent.duration.FiniteDuration

//import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


case class ImmutableSupervisorFactory(minBackoff: FiniteDuration, maxBackoff: FiniteDuration, timeout: FiniteDuration, awaitResult: FiniteDuration)(implicit system: ActorSystem) {
  def actorOf(actorProps: Props, name: String): ActorRef = {
    implicit val _timeout = Timeout(timeout)
    val actorSupervisor: ActorRef =
      system.actorOf(ImmutableSupervisorFactory.props(actorProps, minBackoff, maxBackoff, name = name), name = name + "-immutable-supervisor-factory")
    val feature: Future[CurrentChild] = ask(actorSupervisor, GetCurrentChild).mapTo[CurrentChild]
    Await.result(feature, awaitResult).ref.getOrElse(Actor.noSender)
  }
}

object ImmutableSupervisorFactory {
  def props(actorProps: Props, minBackoff: FiniteDuration, maxBackoff: FiniteDuration, name: String): Props = BackoffSupervisor.props(
    Backoff.onStop(
      actorProps,
      childName = name,
      minBackoff = minBackoff,
      maxBackoff = maxBackoff,
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _: Exception => Restart
      }
    )
  )
}
