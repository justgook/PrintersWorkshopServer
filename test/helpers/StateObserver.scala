/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package helpers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
import akka.testkit.TestProbe
import gnieh.diffson.playJson._
import play.api.libs.json.{JsArray, JsObject, Json}

/**
  * Created by Roman Potashow on 13.07.2016.
  */
class StateObserver(testProbe: TestProbe) extends Actor with ActorLogging {
  var state: JsObject = _
  var revision        = 0

  def receive: Receive = {
    case (array: JsArray, writer: ActorRef) =>
      val patch = JsonPatch.parse(s"$array")
      writer ! TextMessage(s"""{"type":"update", "revision": ${revision + 1}, "args": $array}""")
      state = patch(state).as[JsObject]
    case TextMessage.Strict(str)            =>
      val json = Json.parse(str)
      (json \ "type").as[String] match {
        case "set"     =>
          state = (json \ "args").as[JsObject]
          revision = (json \ "revision").as[Int] //(json \ "revision").asOpt[Int].getOrElse(revision)
          testProbe.ref ! (state, revision)
        case "success" =>
          revision = (json \ "revision").as[Int] //(json \ "revision").asOpt[Int].getOrElse(revision)
          testProbe.ref ! ("success", revision)
        case "patch"   =>
          val patch = JsonPatch.parse(s"${(json \ "args").as[JsArray]}")
          state = patch(state).as[JsObject]
          revision = (json \ "revision").as[Int] //(json \ "revision").asOpt[Int].getOrElse(revision)
          testProbe.ref ! (state, revision)
        case t         => log.warning(s"StateObserver unknown message $str")
      }
    //    case msg@(Connecting | Connected | Disconnecting | Disconnected(None)) => testProbe.ref ! msg
    case msg                                                               => log.error(s"\nUnexpected message: $msg\n")
  }
}

object StateObserver {
  def props(probe: TestProbe) = Props(new StateObserver(probe))
}




