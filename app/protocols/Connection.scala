package protocols

import akka.actor.{Actor, ActorLogging}
import play.api.libs.json.Json

/**
  * Created by Roman Potashow on 30.06.2016.
  */
trait Connection extends Actor with ActorLogging {

}

object Connection {
  implicit val configurationFormat = Json.format[Configuration]
  case class Configuration(name: String = "none", properties: Option[Map[String, String]] = None)
}
