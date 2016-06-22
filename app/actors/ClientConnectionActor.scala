package actors

/**
  * Created by Roman Potashow on 17.06.2016.
  */

import actors.HardwareProtocolsSupportActor.Protocol
import akka.actor.{Actor, ActorRef, Props}
import play.api.Logger

//import play.api.libs.json.{JsNull, JsString, JsValue, Json}
import gnieh.diffson.playJson._
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.libs.json._

// JSON library
import play.api.libs.json.Reads._

// Custom validation helpers
import play.api.libs.functional.syntax._

class ClientConnectionActor(out: ActorRef, connectionRegistry: ActorRef, hardwareProtocolsSupport: ActorRef) extends Actor {


  import ClientConnectionActor._
  import actors.ClientConnectionActor.Command._
//  http://alvinalexander.com/scala/understand-methods-akka-actors-scala-lifecycle
  override def preStart: Unit = {
    connectionRegistry ! ConnectionRegistryActor.Command.Register(out, self)
    hardwareProtocolsSupport ! self
  }



  case class State(protocols: Option[Seq[Protocol]], patchId:Int = 0)
  var state = State(None)
  var patchId = 0
//

  //  http://www.scala-lang.org/api/current/index.html#scala.Enumeration
  //    object WeekDay extends Enumeration {
  //      type WeekDay = Value
  //      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
  //    }

  //  import WeekDay._
  //  def isWorkingDay(d: WeekDay) = ! (d == Sat || d == Sun)
  //  WeekDay.values filter isWorkingDay foreach println
  //  val ackJson = ReadsMatch[Ack]

  def receive = {
    //TODO update to IncomingMessage
    case msg: InEvent => // All data that come from WebSocket
      (msg._type, msg.args) match {
        case ("ping", None) => out ! OutEvent("pong")
        case ("ping", Some(arg)) => out ! OutEvent("pong")
        case _ => out ! OutEvent("error", Json.obj("code" -> "404", "status" -> "Not Found"))
      }

      Logger.info(s"${msg._type}");

    //    case Command.Welcome() =>
    //      val json1 = """{
    //                    |  "a": 1,
    //                    |  "b": true,
    //                    |  "c": "test"
    //                    |}""".stripMargin
    //
    //      val json2 = """{
    //                    |  "a": 6,
    //                    |  "c": "test2",
    //                    |  "d": false
    //                    |}""".stripMargin
    //
    //      val patch = JsonDiff.diff(json1, json2, false)
    //      connectionRegistry ! ConnectionRegistryActor.Command.Broadcast(Json.obj("type" -> "set", "args" -> Json.parse(s"$patch") ))
    case ProtocolsUpdate(protocols) =>
      Logger.debug(s"ClientConnectionActor:ProtocolsUpdate: ${Json.toJson(protocols)}")
      state = State(Some(protocols)) //TODO find how to udpdate State
      out ! OutEvent(_type = "updateProtocols", Json.toJson(protocols))

    case Command.ConnectionsCountChange(count) =>
      Logger.info(s"ClientConnectionActor:Command.ConnectionsCount $count")
    //      out ! Json.obj("type" -> "update", "connections" -> count)
  }
}


// Combinator syntax

object ClientConnectionActor {

  //  http://stackoverflow.com/questions/25223619/how-to-pattern-match-against-a-jsvalue-with-reads
  //  !!ENUMS!!!! http://stackoverflow.com/questions/15488639/how-to-write-readst-and-writest-in-scala-enumeration-play-framework-2-1
  case class InEvent(_type: String, args: Any)

  implicit val InEventReads: Reads[InEvent] = (
    (JsPath \ "type").read[String] and
      (JsPath \ "args").readNullable[JsValue]
    ) (InEvent.apply _)


  //TODO rename it to InClientMessage
  case class OutEvent(_type: String, args: JsValue = JsNull)

  implicit val OutEventWrites: Writes[OutEvent] = (
    (JsPath \ "type").write[String] and
      (JsPath \ "args").write[JsValue]
    ) (unlift(OutEvent.unapply))

  //TODO Update Reads and read _type from type
  //  https://www.playframework.com/documentation/2.5.x/ScalaJsonCombinators

  //TODO remove autoformaters https://www.playframework.com/documentation/2.5.x/ScalaJsonAutomated
//    implicit val inEventFormat = Json.format[InEvent]
  //  implicit val outEventFormat = Json.format[OutEvent]
  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]


  //TODO parse to IncomingMessage in socketController
  //  object IncomingMessageType extends Enumeration {
  //    type IncomingMessageType = Value
  //    val Ping, Update = Value
  //  }
  //
  //  import IncomingMessageType._
  //
  //  case class IncomingMessage(msgType: IncomingMessageType, args: Any)
  //

  sealed trait Command

  object Command {

    case class ConnectionsCountChange(connections: Int) extends Command

    case class ProtocolsUpdate(protocols: Seq[Protocol]) extends Command

    //    case class Broadcast(msg: Any) extends Command
  }

  def props(out: ActorRef, connectionRegistry: ActorRef, hardwareProtocolsSupport: ActorRef) = Props(new ClientConnectionActor(out, connectionRegistry, hardwareProtocolsSupport))
}