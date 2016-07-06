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
  implicit val ProgressFormat      = Json.format[Progress]
  implicit val TemperatureFormat   = Json.format[Temperature]
  implicit val StatusFormat        = Json.format[Status]
  case class Configuration(name: String = "none", properties: Map[String, String] = Map.empty)
  //State definition
  case class Status(text: String = "unknown", file: Option[String] = None /*Change to FileLink*/ , progress: Option[Progress] = None, temperatures: List[Temperature] = List(Temperature()))
  case class Progress(done: Int, of: Int)
  case class Temperature(name: String = "unknown", data: List[Int] = List())

}
