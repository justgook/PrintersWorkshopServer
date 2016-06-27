package actors


import akka.actor.{Actor, ActorLogging, Props}
import play.api.Logger

/**
  * Created by Roman Potashow on 26.06.2016.
  */
//TODO maybe it should not be an Actor - it must hold Ref to actors
class Printer(name: String) extends Actor with ActorLogging {

  import actors.Printer._

  var state = State(settings = Settings(name = name))
  Logger.info(s"printer $state")

  def receive = {
    case _ => Logger.info("not implemented")
  }
}

object Printer {
  //  def props: Props = Props[Printer]
  def props(name: String): Props = Props(new Printer(name))
  case class State(settings: Settings = Settings(), status: Status = Status())
  case class Status(text: String = "unknown", file: Option[String] = None /*Change to FileLink*/ , progress: Option[Progress] = None, temperatures: List[Temperature] = List(Temperature()))
  case class Progress(done: Int, of: Int)
  case class Temperature(name: String = "unknown", data: List[Int] = List(0))
  case class Settings(name: String = "unknown", protocol: Protocol = Protocol())
  case class Protocol(name: String = "none", properties: Option[Map[String, String]] = None)

  /*
    {
      id: "001",
      status: {
        text:"printing",
        file: "test.gcode",
        progress: [231321, 1231321], //printed / total lines of gcode-file
        temperature: [
          [25,30,75,100,170,210,230,220,195,191,193,196,195,195,191,193,196,195,193,196,195],//Extruder 0
          [25,27,29,30,35,39,44,56,58,70,75,73,72,75,70,71,72,75,70,71,12]// Heating Bed
        ]
      },
      settings:{
        name: "Mega Delta",
        protocol: {
          type: "serial",
          args: {
            port: "COM2"
          }
        }
      }

    },
    */


}
