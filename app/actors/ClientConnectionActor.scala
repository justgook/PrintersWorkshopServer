package actors

/**
  * Created by Roman Potashow on 17.06.2016.
  */

import actors.ClientConnectionRegistryActor.ConnectionCountUpdate
import actors.PrinterRegistryActor.{Printer, PrintersList}
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
    connectionRegistry ! Subscribers.Add(self)
    protocolSettings ! Subscribers.Add(self)
    printers ! Subscribers.Add(self)
  }

  out ! Set(state)

  def receive = {
    case Ping       => out ! Pong
    case Reset      => out ! Set(state)
    case Unknown(t) => out ! Fail(s"Unknown type $t")
    //    case PrintersListUpdate(list)     => Logger.info(s"ClientConnectionActor receive PrintersListUpdate $list")
    case ProtocolSettingsUpdate(list) =>
      val newState = state.withProtocols(list).withIncrementPatch()
      out ! patchState(state, newState)
      state = newState
    case ConnectionCountUpdate(c)     =>
      val newState = state.withConnections(c).withIncrementPatch()
      out ! patchState(state, newState)
      state = newState
    case PrintersList(p)              =>
      val newState = state.withPrinters(p).withIncrementPatch()
      out ! patchState(state, newState)
      state = newState
    case msg                          => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}


// Combinator syntax

object ClientConnectionActor {

  implicit val jsonPatchFormat = DiffsonProtocol.JsonPatchFormat
  implicit val stateWrites     = Json.writes[State]
  implicit val stateReads      = Json.reads[State]

  //TODO try move it to parser level
  def patchState(old: State, update: State): Patch = Patch(JsonDiff.diff(old, update, remember = false))

  def props(out: ActorRef, connectionRegistry: ActorRef, protocolSettings: ActorRef, printers: ActorRef) = Props(new ClientConnectionActor(out, connectionRegistry, protocolSettings, printers))
  sealed trait Message
  sealed trait In extends Message
  sealed trait Out extends Message
  case class State(patch: Int = 0, connections: Int = 0, protocols: Option[List[Protocol]] = None, printers: Option[List[Printer]] = None) {
    def withProtocols(p: List[Protocol]) = copy(protocols = Some(p))

    def withPrinters(p: List[Printer]) = copy(printers = Some(p))

    def withIncrementPatch() = copy(patch = patch + 1)

    def withConnections(c: Int) = copy(connections = c)
  }
  case class Update(patch: JsonPatch) extends In
  case class Unknown(msg: String) extends In
  case class Fail(error: String) extends Out
  case class Set(state: State) extends Out
  case class Patch(patch: JsonPatch) extends Out
  case object Ping extends In
  case object Reset extends In
  case object Pong extends Out


  object Formats {


    // in
    implicit val updateReads = Json.reads[Update]

    implicit val inReads = new Reads[In]() {
      override def reads(json: JsValue): JsResult[In] = {
        def read[T: Reads] = implicitly[Reads[T]].reads((json \ "args").get)
        (json \ "type").as[String] match {
          case "update" => read[Update]
          case "ping"   => JsSuccess(Ping)
          case "reset"  => JsSuccess(Reset)
          case t        => JsSuccess(Unknown(t)) // TODO change it to JsError and find way how send it to client .fold()
        }
      }
    }


    // out
    implicit val failWrites  = Json.writes[Fail]
    implicit val patchWrites = Json.writes[Patch]
    implicit val setWrites   = Json.writes[Set]

    implicit val outWrites = new Writes[Out] {
      override def writes(o: Out): JsValue = {
        def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)
        val (t, args) = o match {
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
