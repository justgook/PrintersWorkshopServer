package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{BroadcastRoutingLogic, Router}
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by Roman Potashow on 20.06.2016.
  */

//TODO rename to PrinterTransportSetting
class ProtocolSettingsActor extends Actor {

  var router = Router(BroadcastRoutingLogic())

  import ProtocolSettingsActor._

  def receive = {
    case client: ActorRef       =>
      context watch client
      Logger.debug(s"Registering: $client")
      router = router.removeRoutee(client)
      router = router.addRoutee(client)
      client ! ProtocolSettingsUpdate(List(
        Protocol(
          name = "312312",
          label = "12312",
          properties = List(
            ProtocolProperty(name = "123", _type = "d", enum = Some("1231231"), label = "12", defaultValue = "321"),
            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321")
          ))))
    case Terminated(subscriber) =>
      router = router.removeRoutee(subscriber)
  }
}


object ProtocolSettingsActor {

  implicit val ProtocolPropertyFormat: Format[ProtocolProperty] = (
    (JsPath \ "name").format[String] and
      (JsPath \ "type").format[String] and
      (JsPath \ "label").format[String] and
      (JsPath \ "defaultValue").format[String] and
      (JsPath \ "enum").formatNullable[String]
    ) (ProtocolProperty.apply, unlift(ProtocolProperty.unapply))
  implicit val ProtocolFormat                                   = Json.format[Protocol]

  /*
    object ProtocolPropertyType extends Enumeration {
      type ProtocolPropertyType = Value
      val String, Number, Boolean = Value
    }

    import ProtocolPropertyType._
    case class ProtocolProperty(name: String, _type: ProtocolPropertyType, label: String, defaultValue: String)
    */

  def props: Props = Props[ProtocolSettingsActor]
  sealed trait Message
  case class ProtocolSettingsUpdate(list: List[Protocol]) extends Message
  //Reads.enumNameReads(Gender)
  case class ProtocolProperty(name: String, _type: String, label: String, defaultValue: String, enum: Option[String] = None)
  case class Protocol(name: String, label: String, properties: List[ProtocolProperty])

}
