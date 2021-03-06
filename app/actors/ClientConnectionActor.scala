/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors

import actors.ClientConnectionRegistryActor.ConnectionCountUpdate
import actors.FileRegistryActor.{File, Files}
import actors.PrinterConnectionRegistryActor.PrinterConnections
import actors.PrinterSettingsRegistryActor.{Printer, Printers}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import gnieh.diffson.playJson._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer
import protocols.Connection.Status
import protocols.Protocol.SettingsList
import protocols.{Settings => ProtocolSettings}

import scala.util.{Failure, Success, Try}


class ClientConnectionActor(
                             out: ActorRef,
                             connectionRegistry: ActorRef,
                             protocolSettings: ActorRef,
                             printersSettings: ActorRef,
                             printerConnections: ActorRef,
                             fileRegistry: ActorRef
                           )
  extends Actor with ActorLogging with Stash {

  import ClientConnectionActor._

  private var revision = 1
  private var state = State()

  override def preStart: Unit = {
    connectionRegistry ! Subscribers.Add(self)
    protocolSettings ! Subscribers.Add(self)
    printersSettings ! Subscribers.Add(self)
    printerConnections ! Subscribers.Add(self)
    fileRegistry ! Subscribers.Add(self)
  }

  def receive: Receive = stateBuffering

  def stateBuffering: Receive = {
    var gotSettings = false
    var gotConnection = false
    var gotPrintersSettings = false
    var gotPrintersConnections = false
    var gotFiles = false

    def checkBuffer(state: State): State = {
      if (gotSettings && gotConnection && gotPrintersSettings && gotPrintersConnections && gotFiles) {
        out ! SetState(revision, state)
        unstashAll()
        context.become(standard)
      }
      state
    }

    withDefaultMessages {
      case SettingsList(list)       => gotSettings = true; state = checkBuffer(state.withProtocols(list))
      case ConnectionCountUpdate(c) => gotConnection = true; state = checkBuffer(state.withConnections(c))
      case Printers(p)              => gotPrintersSettings = true; state = checkBuffer(state.withPrinters(p))
      case PrinterConnections(c)    => gotPrintersConnections = true; state = checkBuffer(state.withConditions(c))
      case Files(f)                 => gotFiles = true; state = checkBuffer(state.withFiles(f))
      case Update(_, _)             => stash()
      case msg                      => log.warning(s"got unexpected $msg")
    }
  }

  def standard: Receive = withDefaultMessages {
    case Update(rev, patch) if rev != revision + 1 =>
      log.warning(s"Client sent update with revision $rev when current revision is $revision")
      out ! Fail(Fail.Status.NOT_SYNC)

    case Update(rev, patch)       =>
      (state.withPatch(patch), state) match {
        case (Failure(error), _)           => out ! Fail(Fail.Status.CANNOT_APPLY_PATCH(s"$error"))
        case (Success(newState), oldState) =>
          out ! SuccessUpdate(rev)
          state = newState
          revision = rev
          (newState, oldState) match {
            case (State(_, _, newPrinters, _, _), State(_, _, oldPrinters, _, _)) // Printer update from Client
              if newPrinters != oldPrinters =>
              printersSettings ! Printers(newPrinters)
            case _                          => log.warning("something goes wrong in Update")

          }
      }
    case SettingsList(list)       =>
      val newState = state.withProtocols(list)
      if (newState != state) {
        revision += 1
        out ! Patch(revision, state, newState)
        state = newState
      }
    case ConnectionCountUpdate(c) =>
      val newState = state.withConnections(c)
      if (newState != state) {
        revision += 1
        out ! Patch(revision, state, newState)
        state = newState
      }
    case Printers(p)              =>
      val newState = state.withPrinters(p)
      if (newState != state) {
        revision += 1
        out ! Patch(revision, state, newState)
        state = newState
      }
    case PrinterConnections(list) =>
      val newState = state.withConditions(list)
      if (newState != state) {
        // not update revision for readOnly data
        out ! Patch(revision, state, newState)
        state = newState
      }
    case Files(f)                 =>
      val newState = state.withFiles(f)
      if (newState != state) {
        // not update revision for readOnly data
        out ! Patch(revision, state, newState)
        state = newState
      }
  }

  def withDefaultMessages(fn: Receive): Receive = {
    case Ping       => out ! Pong
    case Reset      => out ! SetState(revision, state)
    case Unknown(_) => out ! Fail(Fail.Status.UNKNOWN_MESSAGE)
    case other      => fn(other)
  }
}


// Combinator syntax

object ClientConnectionActor {

  implicit val jsonPatchFormat = DiffsonProtocol.JsonPatchFormat
  implicit val stateFormat: OFormat[State] = Json.format[State]

  def props(
             out: ActorRef,
             connectionRegistry: ActorRef,
             protocolSettings: ActorRef,
             printersSettings: ActorRef,
             printersConnections: ActorRef,
             fileRegistry: ActorRef)
  = Props(
    new ClientConnectionActor(out, connectionRegistry, protocolSettings, printersSettings, printersConnections, fileRegistry))

  sealed trait Message

  sealed trait In extends Message

  sealed trait Out extends Message

  case class State(connections: Int = 0, protocols: List[ProtocolSettings] = List.empty, printers: Map[String, Printer] = Map.empty, conditions: Map[String, Status] = Map.empty, files: List[File] = List.empty) {
    def withProtocols(p: List[ProtocolSettings]): State = copy(protocols = p)

    def withPrinters(p: Map[String, Printer]): State = copy(printers = p)

    def withConnections(c: Int): State = copy(connections = c)

    def withConditions(c: Map[String, Status]): State = copy(conditions = c)

    def withFiles(f: List[File]): State = copy(files = f)

    def withPatch(p: JsonPatch): Try[State] = Try(p(this))
  }

  case class SetState(revision: Int, state: State) extends Out

  case class SuccessUpdate(revision: Int) extends Out

  case class Update(revision: Int, patch: JsonPatch) extends In

  case class Unknown(msg: String) extends In

  case class Fail(error: Fail.Status) extends Out

  case class Patch(revision: Int, oldState: State, newState: State) extends Out

  object Fail {

    case class Status(code: Int, text: String)

    object Status {
      implicit val failStatusWrites: Writes[Status] = Json.writes[Status]
      val UNKNOWN_MESSAGE = Status(1, "Unknown Message type")
      val NOT_SYNC = Status(2, "You are not in sync with server, please renew your data")

      def CANNOT_APPLY_PATCH(s: String = "Unknown") = Status(3, s)
    }

  }

  // in
  object In {
    implicit val updateReads: Reads[Update] = Json.reads[Update]
    implicit val inReads = new Reads[In]() {
      override def reads(json: JsValue): JsResult[In] = {
        def read[T: Reads] = implicitly[Reads[T]].reads((json \ "args").get)

        (json \ "type").as[String] match {
          case "update" => JsSuccess(Update(revision = (json \ "revision").as[Int], patch = read[JsonPatch].get))
          case "ping"   => JsSuccess(Ping)
          case "reset"  => JsSuccess(Reset)
          case t        => JsSuccess(Unknown(t))
        }
      }
    }
  }

  object Out {
    // out
    implicit val failWrites: Writes[Fail] = Json.writes[Fail]
    implicit val outWrites = new Writes[Out] {
      override def writes(o: Out): JsValue = {
        def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)

        val (t, args, revision) = o match {
          case Fail(error)                  => ("fail", Some(Json.obj("code" -> error.code, "text" -> error.text)), None)
          case Pong                         => ("pong", None, None)
          case Patch(r, oldState, newState) => ("patch", Some(write(
            JsonDiff.diff(oldState, newState, remember = false)
          )), Some(r))
          case SetState(r, state)           => ("set", Some(write(state)), Some(r))
          case SuccessUpdate(r)             => ("success", None, Some(r))
        }
        Json.obj("type" -> t) ++ {
          args.map(data => Json.obj("args" -> data)) getOrElse Json.obj()
        } ++ {
          revision.map(data => Json.obj("revision" -> JsNumber(data))) getOrElse Json.obj()
        }
      }
    }
  }

  object Message {
    implicit val messageFlowTransformer: MessageFlowTransformer[In, Out] = MessageFlowTransformer.jsonMessageFlowTransformer[In, Out]
  }

  case object Ping extends In

  case object Reset extends In

  case object Pong extends Out

}
