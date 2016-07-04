package actors


import actors.PrinterRegistryActor.{PrinterDescription => PrinterSettings}
import akka.actor.{Actor, ActorLogging, Props}
import play.api.Logger
import play.api.libs.json.Json

/**
  * Created by Roman Potashow on 26.06.2016.
  */
//TODO maybe it should not be an Actor - it must hold Ref to connection actor
//TOTO btw do i need that class at all ?
class PrinterActor(settings: PrinterSettings) extends Actor with ActorLogging {

  import actors.PrinterActor._

  var status = Status()
  //  var settings = PrinterSettings()
  Logger.info(s"Created new Printer $status")
  context.parent ! PrinterStateUpdate(status)

  def receive = {
    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}

object PrinterActor {

  implicit val ProgressFormat    = Json.format[Progress]
  implicit val TemperatureFormat = Json.format[Temperature]
  implicit val StatusFormat      = Json.format[Status]

  def props(settings: PrinterSettings): Props = Props(new PrinterActor(settings))
  sealed trait Message
  case class PrinterStateUpdate(status: Status) extends Message

  //State definition
  case class Status(text: String = "unknown", file: Option[String] = None /*Change to FileLink*/ , progress: Option[Progress] = None, temperatures: List[Temperature] = List(Temperature()))
  case class Progress(done: Int, of: Int)
  case class Temperature(name: String = "unknown", data: List[Int] = List())

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
