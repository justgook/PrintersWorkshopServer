package actors


import actors.PrinterActor.{PrinterStateUpdate, Status => PrinterStatus}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.Logger
import play.api.libs.json.Json
import protocols.Connection.Configuration

/**
  * Created by Roman Potashow on 26.06.2016.
  */
class PrinterRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.PrinterRegistryActor._

  //TODO add way to save and store on restart not printers pat Printers it self
  //TODO update it to router with akka.routing.ConsistentHashingRoutingLogic
  private var printersSettings: Map[ActorRef, PrinterDescription] = Map()
  private var printersStates  : Map[ActorRef, PrinterStatus]      = Map()
  private var printer2Id      : Map[ActorRef, Int]                = Map()
  private var Id2printer      : Map[Int, ActorRef]                = Map()

  private var lastId                              = 0
  private var printers: Map[Int, PrinterInstance] = Map()

  override def afterAdd(client: ActorRef): Unit = {
    client ! outPrintersList(printersSettings, printersStates, printer2Id)
  }

  //TODO - remove me when it is implemented in client / restore from save
  self ! CreateNewPrinter(name = "Kossel Mini Bugagag")

  def receive = withSubscribers {
    case RestorePrinter(s) =>
      lastId += 1
      val ref = protocols.connect(s.protocol, context)
      printers += lastId -> PrinterInstance(description = s, connection = Some(ref))
      Logger.info(s"RestorePrinter $printers")

      //TODO remove useless maps!!
      printersSettings += (ref -> s)
      printersStates += (ref -> PrinterStatus())

      printer2Id += (ref -> lastId)
      Id2printer += (lastId -> ref)

    case CreateNewPrinter(name)    =>
      lastId += 1
      val s = PrinterDescription(name = name)
      printers += (lastId -> PrinterInstance(s))
      Logger.info(s"RestorePrinter $printers")
      val ref = protocols.connect(Configuration(name = "serialport"), context)
      printersSettings += (ref -> s)
      printersStates += (ref -> PrinterStatus())

      printer2Id += (ref -> lastId)
      Id2printer += (lastId -> ref)
      subscribers.route(outPrintersList(printersSettings, printersStates, printer2Id), self)
    case PrinterStateUpdate(state) =>
      val ref = sender()
      printersStates += (ref -> state)
      subscribers.route(outPrintersList(printersSettings, printersStates, printer2Id), self)

    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
  //  name: "Kossel Mini",
  //  protocol: {type: "none"} // Not set any configs
  case class PrinterInstance(
                              description: PrinterDescription,
                              printerStatus: Option[PrinterStatus] = None,
                              connection: Option[ActorRef] = None)
}

object PrinterRegistryActor {

  implicit val settingsFormat = Json.format[PrinterDescription]
  implicit val printerFormat  = Json.format[Printer]

  def props: Props = Props[PrinterRegistryActor]

  def outPrintersList(printersSettings: Map[ActorRef, PrinterDescription],
                      printersStates: Map[ActorRef, PrinterStatus],
                      printer2Id: Map[ActorRef, Int]): PrintersList = {
    val list = printer2Id.map {
      case (ref, id) =>
        Printer(id = id, settings = printersSettings(ref), status = printersStates(ref))
    }(collection.breakOut): List[Printer]
    PrintersList(list = list)
  }
  sealed trait Message
  sealed trait FromClient extends Message
  sealed trait ToClient extends Message

  case class PrinterDescription(name: String = "unknown", protocol: Configuration = Configuration())

  case class Printer(id: Int, settings: PrinterDescription, status: PrinterStatus)
  case class RestorePrinter(settings: PrinterDescription) extends FromClient
  //Can come form clients (as load from settings) or from file/db (when app is restarted)
  case class CreateNewPrinter(name: String) extends FromClient
  case class UpdatePrinter(id: Int, settings: PrinterDescription) extends FromClient
  case class PrintersList(list: List[Printer]) extends ToClient
}
