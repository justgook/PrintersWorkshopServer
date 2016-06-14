package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.JsValue


// play-json  TODO implement diffs for json objects // https://github.com/gnieh/diffson
//import gnieh.diffson.playJson._

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

//case class MyData1(a: int, b: String)
//object MyData1 {
//
//}

class MyWebSocketActor(out: ActorRef) extends Actor {
//  var state: AnyRef
  def receive = {
    case msg: JsValue =>
      out ! msg
  }
}
