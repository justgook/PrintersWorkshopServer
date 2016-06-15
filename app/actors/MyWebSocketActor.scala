package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.{JsObject, JsValue}
import ConnectionRegistryActor._

// play-json  TODO implement diffs for json objects // https://github.com/gnieh/diffson
//import gnieh.diffson.playJson._

object MyWebSocketActor {
  def props(out: ActorRef, connectionRegistry: ActorRef) = Props(new MyWebSocketActor(out, connectionRegistry))
}

//case class MyData1(a: int, b: String)
//object MyData1 {
//
//}

class MyWebSocketActor(out: ActorRef, connectionRegistry: ActorRef) extends Actor {
//  var state: AnyRef
  def receive = {
    case msg: JsValue =>
      out ! msg


      // broadcast example
      val msgType = (msg \ "type").asOpt[String]
      val msgArgs = (msg \ "args").toOption

      (msgType, msgArgs) match {
        case (Some("broadcast"), Some(args)) => connectionRegistry ! Command.Broadcast(args)
      }

  }
}
