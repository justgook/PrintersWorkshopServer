package protocols


import akka.actor.{Actor, ActorLogging}
import play.api.libs.json._

//import protocols.Connection.StatusText.StatusText

/**
  * Created by Roman Potashow on 30.06.2016.
  */
trait Connection extends Actor with ActorLogging {}


object StatusText extends Enumeration {
  type StatusText = Value
  val Unknown    = Value("unknown")
  val Connecting = Value("connecting")
  val Connected  = Value("connected")
  val Ready      = Value("ready")
  val Printing   = Value("printing")
  implicit val StatusTextFormat = new Format[StatusText] {
    def writes(myEnum: StatusText) = JsString(myEnum.toString)

    def reads(json: JsValue) = JsSuccess(StatusText.withName(json.as[String]))
  }
}

object Connection {


  import StatusText._

  implicit val configurationFormat = Json.format[Configuration]
  implicit val ProgressFormat      = Json.format[Progress]
  implicit val TemperatureFormat   = Json.format[Temperature]
  implicit val StatusFormat        = Json.format[Status]

  case class Configuration(name: String = "none", properties: Map[String, String] = Map.empty)

  case class Status(
                     text: StatusText = Unknown,
                     file: Option[String] = None, //Change to FileLink
                     progress: Option[Progress] = None, //ReadOnly Data
                     temperatures: List[Temperature] = List(Temperature()) //ReadOnly Data
                   ) {

    def withFile(file: String) = copy(file = Some(file))

    def readyToPrint() = withText(Ready)

    def withText(t: StatusText) = copy(text = t)
  }


  case class Progress(done: Int, of: Int)
  case class Temperature(name: String = "unknown", data: List[Int] = List())
  //State definition

}
