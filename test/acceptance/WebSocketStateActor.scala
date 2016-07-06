package acceptance

import acceptance.WebSocketClient.Messages.TextMessage
import akka.actor.{Actor, Props}
import akka.testkit.TestProbe
import gnieh.diffson.playJson._
import play.api.libs.json.{JsArray, JsObject, Json}

/**
  * Created by Roman Potashow on 06.07.2016.
  */
class WebSocketStateActor(handle: TestProbe) extends Actor {
  var state: JsObject = Json.obj()

  def receive = {
    case TextMessage(str) =>
      val json = Json.parse(str)
      (json \ "type").as[String] match {
        case "set"   =>
          state = (json \ "args").as[JsObject]
          handle.ref ! state
        case "patch" =>
          val patch = JsonPatch.parse(s"${(json \ "args").as[JsArray]}")
          state = patch(state).as[JsObject]
          handle.ref ! state
        case t       => println(s"WebSocketStateActor got unknown type  - $t")
      }
    case msg              => handle.ref ! msg

  }
}
object WebSocketStateActor {
  def props(handle: TestProbe) = Props(new WebSocketStateActor(handle))

  //  def props: Props = Props[WebSocketStateActor]

  //  def apply()(implicit system: ActorSystem) = new WebSocketStateActor(system)
  //  def apply(name: String)(implicit system: ActorSystem) = new WebSocketStateActor(system, name)
}
