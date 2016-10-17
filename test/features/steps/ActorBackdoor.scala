/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package features.steps

import akka.actor.{ActorIdentity, ActorRef, ActorSystem, Identify}
import akka.testkit.TestProbe
import akka.util.Timeout
import cucumber.api.scala.{EN, ScalaDsl}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
  * Created by gook on 17.10.2016.
  */
trait ActorBackdoor extends ScalaDsl with EN {
  implicit def system: ActorSystem

  var probe: TestProbe

  When("""^Actor with name "([^"]*)" dies$""") { (name: String) =>
    implicit val timeout = Timeout(2.seconds)
    probe = TestProbe()
    val future: Future[ActorRef] = system.actorSelection("/user/" + name + "-immutable-supervisor-factory/" + name).resolveOne()
    val targetActor = Await.result(future, timeout.duration)
    probe watch targetActor
    targetActor ! new IllegalArgumentException
  }
  Then("""^Actor with name "([^"]*)" must be live$""") { (name: String) =>
    val identifyId = Random.nextInt(6)
    val select = system.actorSelection("/user/" + name + "-immutable-supervisor-factory/" + name) // ! Identify(identifyId)
    select.tell(Identify(identifyId), probe.ref)
    probe.expectMsgPF() {
      case ActorIdentity(`identifyId`, _) => true
    }
  }
}
