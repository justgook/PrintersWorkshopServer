package protocols


import actors.Subscribers
import akka.actor.{Actor, ActorLogging}
import play.api.libs.json._

//import protocols.Connection.StatusText.StatusText

/**
  * Created by Roman Potashow on 30.06.2016.
  */
trait Connection extends Actor with ActorLogging with Subscribers {}

//switch (value) {
//case "connected":
//return <Checkmark />;
//case "ready":
//return <DocumentVerified />;
//case "printing":
//return <Prining />;
//case "editing":
//return <Lock />;
//case "updating":
//case "reconnecting":
//return <Spinning />;
//case "no-сonfig":
//return <NoConfig />;
//case "pause":
//return <Pause />;
//default:
//return null; //TODO set some default value


object StatusText extends Enumeration {
  type StatusText = Value
  val Unknown    = Value("unknown")
  val Remove     = Value("remove")
  val Editing    = Value("editing")
  val Connecting = Value("connecting")

  val Connected = Value("connected")
  val Ready     = Value("ready")
  val Printing  = Value("printing")
  implicit val StatusTextFormat = new Format[StatusText] {
    def writes(myEnum: StatusText) = JsString(myEnum.toString)

    def reads(json: JsValue) = JsSuccess(StatusText.withName(json.as[String]))
  }
}

object Connection {
  //  sealed trait Command
  //  object Kill extends Command
  //  implicit val configurationFormat = Json.format[Configuration]
  implicit val progressFormat    = Json.format[Progress]
  implicit val temperatureFormat = Json.format[Temperature]
  implicit val statusFormat      = Json.format[Status]
  case class ConsoleInput(in: String)
  case class Status(progress: Option[Progress] = None, //ReadOnly Data
                    temperatures: Option[List[Temperature]] = None //ReadOnly Data
                   )
  case class Progress(done: Int, of: Int)
  case class Temperature(name: String = "unknown", data: List[Int] = List())
  //  object ConsoleInput {
  //    implicit val consoleInputReads = Json.format[ConsoleInput]
  //  }
}
