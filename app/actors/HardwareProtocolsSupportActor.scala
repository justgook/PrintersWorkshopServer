package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}

//import akka.persistence.PersistentActor
import akka.routing.{BroadcastRoutingLogic, Router}
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by Roman Potashow on 20.06.2016.
  */
class HardwareProtocolsSupportActor extends Actor {

  var router = Router(BroadcastRoutingLogic())

  def broadcast(m: Any) = {
    router.route(m, self)
  }

  //  import HardwareProtocolsSupportActor._


  def receive = {
    case client: ActorRef =>
      context watch client
      Logger.debug(s"Registering: $client")
      router = router.removeRoutee(client)
      router = router.addRoutee(client)


    //      client ! List(
    //        Protocol(
    //          name = "312312",
    //          label = "12312",
    //          properties = Seq(
    //            ProtocolProperty(name = "123", _type = "d", enum = Some("1231231"), label = "12", defaultValue = "321"),
    //            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
    //            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
    //            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
    //            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321"),
    //            ProtocolProperty(name = "123", _type = "d", label = "12", defaultValue = "321")
    //          )))
    case Terminated(subscriber) =>
      router = router.removeRoutee(subscriber)
  }
}


object HardwareProtocolsSupportActor {


  /*
    object ProtocolPropertyType extends Enumeration {
      type ProtocolPropertyType = Value
      val String, Number, Boolean = Value
    }

    import ProtocolPropertyType._
    case class ProtocolProperty(name: String, _type: ProtocolPropertyType, label: String, defaultValue: String)
    */


  //  implicit val ProtocolPropertyFormat = Json.format[ProtocolProperty]

  implicit val ProtocolPropertyFormat: Format[ProtocolProperty] = (
    (JsPath \ "name").format[String] and
      (JsPath \ "type").format[String] and
      (JsPath \ "label").format[String] and
      (JsPath \ "defaultValue").format[String] and
      (JsPath \ "enum").formatNullable[String]
    ) (ProtocolProperty.apply, unlift(ProtocolProperty.unapply))

  implicit val ProtocolFormat = Json.format[Protocol]
  //Reads.enumNameReads(Gender)
  case class ProtocolProperty(name: String, _type: String, label: String, defaultValue: String, enum: Option[String] = None)

  case class Protocol(name: String, label: String, properties: Seq[ProtocolProperty])


  def props: Props = Props[HardwareProtocolsSupportActor]

}
