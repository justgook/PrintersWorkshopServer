package actors

/**
  * Created by Roman Potashow on 17.06.2016.
  */

import actors.ConnectionRegistryActor.ConnectionCountUpdate
import actors.PrintersActor.PrintersListUpdate
import actors.ProtocolSettingsActor.{Protocol, ProtocolSettingsUpdate}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gnieh.diffson.playJson._
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer


class ClientConnectionActor(out: ActorRef, connectionRegistry: ActorRef, protocolSettings: ActorRef, printers: ActorRef)
  extends Actor with ActorLogging {

  import ClientConnectionActor._

  private var state = State()

  override def preStart: Unit = {
    connectionRegistry ! self
    protocolSettings ! self
    printers ! self
  }
  out ! Set(state)

  def receive = {
    case Ping                         => out ! Pong
    case Reset                        => out ! Set(state)
    case SimpleCommandNoArgs          => out ! Fail("not implemented")
    case XXX(a)                       => out ! ZZZ(a)
    case YYY(a)                       => out ! ZZZ(a + " hm-hm-hm")
    case Unknown(t)                   => out ! Fail(s"Unknown type $t")
    case PrintersListUpdate(list)     => Logger.info(s"ClientConnectionActor receive PrintersListUpdate $list")
    case ProtocolSettingsUpdate(list) =>
      val newState = state.withProtocols(list).withIncrementPatch()
      out ! patchState(state, newState)
      state = newState
    case ConnectionCountUpdate(c)     =>
      val newState = state.withConnections(c).withIncrementPatch()
      out ! patchState(state, newState)
      state = newState
    case _                            => Logger.warn(s"ClientConnectionActor got unknown message from ${sender()}")
  }
}


// Combinator syntax

object ClientConnectionActor {
  //  JsonPatch.Format
  implicit val jsonPatchFormat = DiffsonProtocol.JsonPatchFormat
  implicit val stateFormat     = Json.format[State]

  def patchState(old: State, update: State): Patch = Patch(JsonDiff.diff(old, update, remember = false))

  def props(out: ActorRef, connectionRegistry: ActorRef, protocolSettings: ActorRef, printers: ActorRef) = Props(new ClientConnectionActor(out, connectionRegistry, protocolSettings, printers))
  sealed trait Message
  sealed trait In extends Message
  sealed trait Out extends Message
  case class State(patch: Int = 0, connections: Int = 0, protocols: Option[List[Protocol]] = None) {
    def withProtocols(p: List[Protocol]) = copy(protocols = Some(p))

    def withIncrementPatch() = copy(patch = patch + 1)

    def withConnections(c: Int) = copy(connections = c)
  }
  case class XXX(a: String) extends In
  case class YYY(b: String) extends In
  case class Unknown(msg: String) extends In
  case class ZZZ(b: String) extends Out
  case class Fail(error: String) extends Out
  case class Set(state: State) extends Out
  case class Patch(patch: JsonPatch) extends Out
  case object SimpleCommandNoArgs extends In
  case object Ping extends In
  case object Reset extends In


  case object Pong extends Out

  object Formats {

    // in
    implicit val xxxReads = Json.reads[XXX]
    implicit val yyyReads = Json.reads[YYY]

    implicit val inReads = new Reads[In]() {
      override def reads(json: JsValue): JsResult[In] = {
        def read[T: Reads] = implicitly[Reads[T]].reads((json \ "args").get)
        (json \ "type").as[String] match {
          case "xxx"                    => read[XXX]
          case "yyy"                    => read[YYY]
          case "simple.command.no.args" => JsSuccess(SimpleCommandNoArgs)
          case "ping"                   => JsSuccess(Ping)
          case "reset"                  => JsSuccess(Reset)
          case t                        => JsSuccess(Unknown(t)) // TODO change it to JsError and find way how send it to client .fold()
        }
      }
    }


    // out
    implicit val zzzWrites   = Json.writes[ZZZ]
    implicit val failWrites  = Json.writes[Fail]
    implicit val patchWrites = Json.writes[Patch]
    implicit val setWrites   = Json.writes[Set]

    implicit val outWrites = new Writes[Out] {
      override def writes(o: Out): JsValue = {
        def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)
        val (t, args) = o match {
          case x: ZZZ      => ("xxx", Some(write(x)))
          case error: Fail => ("fail", Some(write(error)))
          case Pong        => ("pong", None)
          case p: Patch    => ("patch", Some(write(p)))
          case state: Set  => ("set", Some(write(state)))
        }
        Json.obj("type" -> t) ++ {
          args.map(args => Json.obj("args" -> args)) getOrElse Json.obj()
        }
      }
    }

    implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[In, Out]

  }
}