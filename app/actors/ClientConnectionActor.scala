package actors

/**
  * Created by Roman Potashow on 17.06.2016.
  */


import actors.ClientConnectionRegistryActor.ConnectionCountUpdate
import actors.PrinterRegistryActor.{PrinterData, PrinterDataList}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gnieh.diffson.playJson._
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer
import protocols.Protocol.SettingsList
import protocols.{Settings => ProtocolSettings}
import protocols.JsonFormats.SettingsJson._


class ClientConnectionActor(out: ActorRef, connectionRegistry: ActorRef, protocolSettings: ActorRef, printers: ActorRef)
  extends Actor with ActorLogging {

  import ClientConnectionActor._

  private var state = State()

  override def preStart: Unit = {
    connectionRegistry ! Subscribers.Add(self)
    protocolSettings ! Subscribers.Add(self)
    printers ! Subscribers.Add(self)
  }

  out ! SetState(state)

  def receive = {
    case Ping                     => out ! Pong
    case Reset                    => out ! SetState(state) // TODO add request to update from all registry (connectionRegistry, protocolSettings, printers)
    case Unknown(t)               => out ! Fail(s"Unknown type $t")
    case Update(patch)            => Logger.info(s"ClientConnectionActor Update received, ${Formats.patchUpdate(patch, state)} ")
    case SettingsList(list)       =>
      val newState = state.withProtocols(list).withIncrementPatch()
      out ! Patch(state, newState)
      state = newState
    case ConnectionCountUpdate(c) =>
      val newState = state.withConnections(c).withIncrementPatch()
      out ! Patch(state, newState)
      state = newState
    case PrinterDataList(p)       =>
      val newState = state.withPrinters(p).withIncrementPatch()
      out ! Patch(state, newState)
      state = newState

    case printer: PrinterData =>
      val printers = state.printers find (_.id == printer.id) match {
        case None    => printer :: state.printers
        case Some(p) => printer :: state.printers.dropWhile(_.id == printer.id)
      }
      val newState = state.withPrinters(printers).withIncrementPatch()
      out ! Patch(state, newState)
      state = newState
    case msg                  => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}


// Combinator syntax

object ClientConnectionActor {

  def props(out: ActorRef, connectionRegistry: ActorRef, protocolSettings: ActorRef, printers: ActorRef) = Props(new ClientConnectionActor(out, connectionRegistry, protocolSettings, printers))

  //TODO re-implement me in printer-list parsing from client for sending updates to the PrinterRegistry
  def getListDiff(list1: List[Any], list2: List[Any]) = {
    val unwanted = list2.toSet
    list1.filterNot(unwanted)
  }

  sealed trait Message
  sealed trait In extends Message
  sealed trait Out extends Message
  case class State(patch: Int = 0, connections: Int = 0, protocols: List[ProtocolSettings] = List.empty, printers: List[PrinterData] = List.empty) {
    def withProtocols(p: List[ProtocolSettings]) = copy(protocols = p)

    def withPrinters(p: List[PrinterData]) = copy(printers = p)

    def withIncrementPatch() = copy(patch = patch + 1)

    def withConnections(c: Int) = copy(connections = c)
  }
  case class SetState(state: State) extends Out
  case class Update(patch: JsonPatch) extends In
  case class Unknown(msg: String) extends In
  case class Fail(error: String) extends Out
  case class Patch(oldState: State, newState: State) extends Out
  case object Ping extends In
  case object Reset extends In
  case object Pong extends Out


  object Formats {
    implicit val jsonPatchFormat = DiffsonProtocol.JsonPatchFormat
    implicit val stateWrites     = Json.writes[State]
    implicit val stateReads      = Json.reads[State]

    // in
    implicit val updateReads = Json.reads[Update]

    implicit val inReads = new Reads[In]() {
      override def reads(json: JsValue): JsResult[In] = {
        def read[T: Reads] = implicitly[Reads[T]].reads((json \ "args").get)
        (json \ "type").as[String] match {
          case "update" => JsSuccess(Update(patch = read[JsonPatch].get)) //read[Update]
          case "ping"   => JsSuccess(Ping)
          case "reset"  => JsSuccess(Reset)
          case t        => JsSuccess(Unknown(t)) // TODO change it to JsError and find way how send it to client .fold()
        }
      }
    }


    // out
    implicit val failWrites = Json.writes[Fail]

    implicit val outWrites = new Writes[Out] {
      override def writes(o: Out): JsValue = {
        def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)
        val (t, args) = o match {
          case error: Fail               => ("fail", Some(write(error)))
          case Pong                      => ("pong", None)
          case Patch(oldState, newState) => ("patch", Some(write(
            JsonDiff.diff(oldState, newState, remember = false)
          )))
          case SetState(state)           => ("set", Some(write(state)))
        }
        Json.obj("type" -> t) ++ {
          args.map(args => Json.obj("args" -> args)) getOrElse Json.obj()
        }
      }
    }
    implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[In, Out]

    def patchUpdate(patch: JsonPatch, oldState: State): State = {
      patch(oldState)
    }

  }
}
