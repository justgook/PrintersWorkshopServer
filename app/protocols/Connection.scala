package protocols


import akka.actor.{Actor, ActorLogging}
import play.api.libs.json._

//import protocols.Connection.StatusText.StatusText

/**
  * Created by Roman Potashow on 30.06.2016.
  */
trait Connection extends Actor with ActorLogging {}

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

  implicit val configurationFormat = Json.format[Configuration]
  implicit val ProgressFormat      = Json.format[Progress]
  implicit val TemperatureFormat   = Json.format[Temperature]
  implicit val StatusFormat        = Json.format[Status]

  case class Configuration(name: String = "none", properties: Map[String, String] = Map.empty)

  case class Status(progress: Option[Progress] = None, //ReadOnly Data
                    temperatures: Option[List[Temperature]] = None //ReadOnly Data
                   )


  case class Progress(done: Int, of: Int)
  case class Temperature(name: String = "unknown", data: List[Int] = List())
  //State definition

}
