package actors


import actors.PrinterActor.{PrinterStateUpdate, Status => PrinterStatus}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.Logger
import play.api.libs.json.Json

/**
  * Created by Roman Potashow on 26.06.2016.
  */
class PrinterRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.PrinterRegistryActor._

  //TODO add way to save and store on restart not printers pat Printers it self
  //TODO update it to router with akka.routing.ConsistentHashingRoutingLogic
  private var printersSettings: Map[ActorRef, Settings]      = Map()
  private var printersStates  : Map[ActorRef, PrinterStatus] = Map()
  private var printer2Id      : Map[ActorRef, Int]           = Map()
  private var Id2printer      : Map[Int, ActorRef]           = Map()
  private var lastId                                         = 0

  //  private var printersID: Map[Int, ActorRef] = Map()
  //  private var printers2 = Router(ConsistentHashingRoutingLogic.defaultAddress())
  //TODO - remove me when it is implemented in client / restore from save
  self ! CreateNewPrinter(name = "name")

  override def afterAdd(client: ActorRef): Unit = {
    client ! outPrintersList(printersSettings, printersStates, printer2Id)
  }

  def receive = withSubscribers {
    case RestorePrinter(s)         =>
      val ref = context.actorOf(PrinterActor.props(settings = s))
      printersSettings += (ref -> s)
      printersStates += (ref -> PrinterStatus())
      lastId += 1
      printer2Id += (ref -> lastId)
      Id2printer += (lastId -> ref)
    case CreateNewPrinter(name)    =>
      val s = Settings(name = name)
      val ref = context.actorOf(PrinterActor.props(settings = s))
      printersSettings += (ref -> s)
      printersStates += (ref -> PrinterStatus())
      lastId += 1
      printer2Id += (ref -> lastId)
      Id2printer += (lastId -> ref)
      subscribers.route(outPrintersList(printersSettings, printersStates, printer2Id), self)
    case PrinterStateUpdate(state) =>
      val ref = sender()
      printersStates += (ref -> state)
      subscribers.route(outPrintersList(printersSettings, printersStates, printer2Id), self)

    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}

object PrinterRegistryActor {
  implicit val protocolFormat = Json.format[Protocol]
  implicit val settingsFormat = Json.format[Settings]
  implicit val printerFormat  = Json.format[Printer]

  def props: Props = Props[PrinterRegistryActor]

  def outPrintersList(printersSettings: Map[ActorRef, Settings],
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
  case class Protocol(name: String = "none", properties: Option[Map[String, String]] = None)
  case class Settings(name: String = "unknown", protocol: Protocol = Protocol())
  case class Printer(id: Int, settings: Settings, status: PrinterStatus)
  case class RestorePrinter(settings: Settings) extends FromClient
  //Can come form clients (as load from settings) or from file/db (when app is restarted)
  case class CreateNewPrinter(name: String) extends FromClient
  case class UpdatePrinter(id: Int, settings: Settings) extends FromClient
  case class PrintersList(list: List[Printer]) extends ToClient
}
