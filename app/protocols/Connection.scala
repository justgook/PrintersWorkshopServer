/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

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


sealed trait StatusText extends Product {
  lazy val code: String = productPrefix.toLowerCase
}

object StatusText extends {

  private val Values = Set(Unknown, Remove, Editing, Connecting, Connected, Ready, Printing)
  private val codeMap: Map[String, StatusText with Serializable] = Values.map(x => (x.code, x)).toMap

  case object Unknown extends StatusText

  case object Remove extends StatusText

  case object Editing extends StatusText

  case object Connecting extends StatusText

  case object Connected extends StatusText

  case object Ready extends StatusText

  case object Printing extends StatusText


  implicit val JsonFormat = new Format[StatusText]() {
    override def reads(json: JsValue): JsResult[StatusText] = {
      def lookup(code: String): JsResult[StatusText] = {
        codeMap.get(code) match {
          case None         => JsError(s"Unknown code $code")
          case Some(status) => JsSuccess(status)
        }
      }

      json.validate[String] flatMap lookup
    }

    override def writes(o: StatusText): JsValue = JsString(o.code)
  }
}


//
//object StatusText extends Enumeration {
//  type StatusText = Value
//  val Unknown = Value("unknown")
//  val Remove = Value("remove")
//  val Editing = Value("editing")
//  val Connecting = Value("connecting")
//
//  val Connected = Value("connected")
//  val Ready = Value("ready")
//  val Printing = Value("printing")
//  implicit val StatusTextFormat = new Format[StatusText] {
//    def writes(myEnum: StatusText) = JsString(myEnum.toString)
//
//    def reads(json: JsValue): JsSuccess[StatusText.Value] = {
//      val result = StatusText.values.find(_.toString == json.as[String]).getOrElse(Unknown)
//      JsSuccess(result)
//    }
//  }
//}

object Connection {
  //  sealed trait Command
  //  object Kill extends Command
  //  implicit val configurationFormat = Json.format[Configuration]
  implicit val progressFormat: OFormat[Progress] = Json.format[Progress]
  implicit val temperatureFormat: OFormat[Temperature] = Json.format[Temperature]
  implicit val statusFormat: OFormat[Status] = Json.format[Status]

  case class ConsoleInput(in: String)

  case class Status(progress: Option[Progress] = None, //ReadOnly Data
                    temperatures: Option[List[Temperature]] = None //ReadOnly Data
                   )

  case class Progress(done: Int, of: Int)

  case class Temperature(name: String = "unknown", data: List[Int] = List())

}
