package acceptance.helpers

import acceptance.helpers.WebSocketClient.Messages._
import akka.actor.{Actor, ActorLogging, Props}
import akka.testkit.TestProbe
import gnieh.diffson.playJson._
import play.api.libs.json.{JsArray, JsObject, Json}

/**
  * Created by Roman Potashow on 13.07.2016.
  */
class StateObserver(testProbe: TestProbe) extends Actor with ActorLogging {
  var state: JsObject = Json.obj()

  override def receive: Receive = {
    case array: JsArray                                              =>
      val patch = JsonPatch.parse(s"$array")
      state = patch(state).as[JsObject]
    case TextMessage(str)                                            =>
      val json = Json.parse(str)
      (json \ "type").as[String] match {
        case "set"     =>
          state = (json \ "args").as[JsObject]
          testProbe.ref ! (state, (json \ "revision").as[Int])
        case "success" =>
          testProbe.ref ! (state, (json \ "revision").as[Int])
        case "patch"   =>
          val patch = JsonPatch.parse(s"${(json \ "args").as[JsArray]}")
          state = patch(state).as[JsObject]
          testProbe.ref ! (state, (json \ "revision").as[Int])
        case t         => log.warning(s"StateObserver unknown message $str")
      }
    case Connecting | Connected | Disconnecting | Disconnected(None) =>
    case msg                                                         => log.error(s"\nUnexpected message: $msg\n")
  }
}

object StateObserver {
  def props(probe: TestProbe) = Props(new StateObserver(probe))
}




