package actors


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import play.api.Logger
import play.api.libs.json.Json
import protocols.Connection.{Configuration, Status => PrinterConnectionStatus}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Roman Potashow on 26.06.2016.
  */
class PrinterRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.PrinterRegistryActor._


  //TODO add way to save and store on restart Application

  private var lastId                                   = 0
  private var printers     : Map[Int, PrinterInstance] = Map.empty
  private var connection2id: Map[ActorRef, Int]        = Map()

  override def afterAdd(client: ActorRef): Unit = {
    client ! PrinterDataList.fromMap(printers)
  }

  def receive = withSubscribers {
    case PrinterDataList(list) =>
      list map {
        //TODO  Create cache for that part - and got only diff od it
        //      def getListDiff(list1: List[Any], list2: List[Any]) = {
        //        val unwanted = list2.toSet
        //        list1.filterNot(unwanted)
        //      }
        case PrinterData(None, Some(settings), _) => //Create new printer
          lastId += 1
          var printer = lastId -> PrinterInstance(settings)
          settings.config match {
            case Some(config) =>
              val ref = protocols.connect(config, context)
              connection2id += ref -> lastId
              printer = lastId -> printer._2.withConnection(ref)
            case None         =>
          }
          printers += printer
          subscribers.route(PrinterDataList.fromMap(printers), self)

        case PrinterData(Some(id), Some(settings), _) => //Update Printer printer
          printers.get(id) match {
            //TODO remove that id at all, and just update each time all data (if needed)
            case None          => Logger.warn(s"Printer with id $id not found")
            case Some(printer) =>
              //              Logger.error(s"NEED IMPLEMENT Printer Settings update $settings")
              (settings, printer.description.config) match {
                case (PrinterDescription(name, Some(config)), Some(printerConfig))
                  if printerConfig != config                         => //config updated
                  printer.connection match {
                    case Some(connection) => // Connection exist for that printer
                      Logger.error(s"NEED IMPLEMENT RECONNECT with new $config")
                      implicit val timeout = Timeout(1.seconds)
                      //TODO replace with real message for status update
                      val future = connection ? "hello"
                      val status = Await.result(future, timeout.duration).asInstanceOf[PrinterConnectionStatus]
                      printers += (id -> printer.withStatus(status))
                      subscribers.route(PrinterDataList.fromMap(printers), self)
                    case None             => // Connection NOT exist for that printer
                      Logger.error(s"NEED IMPLEMENT CONNECT with new $config")
                      subscribers.route(PrinterDataList.fromMap(printers), self)
                  }
                case (PrinterDescription(name, Some(config)), None)  =>
                  protocols.connect(config, context)
                  Logger.error(s"NEED IMPLEMENT CONNECT with new $config")
                case (PrinterDescription(name, Some(config)), Some(printerConfig))
                  if printerConfig == config                         =>
                  Logger.error("IF configs is same no need to reconnect! maybe just ignore that case")
                case (PrinterDescription(name, None), printerConfig) =>
                  Logger.info("Just Update Name")
              }
          }
        case _                                        => ""
      }

    case status: PrinterConnectionStatus => //update Status form connection
      val ref = sender()
      connection2id.get(ref) match {
        case Some(id) => printers.get(id) match {
          case Some(printer) =>
            Logger.info("PrinterConnectionStatus => update Status form connection")
            printers += (id -> printer.withStatus(status))
            subscribers.route(PrinterDataList.fromMap(printers), self)
          case None          => Logger.warn(s"${self.path.name}(${this.getClass.getName}) no printer with ID $id")
        }
        case None     => Logger.warn(s"${self.path.name}(${this.getClass.getName}) no id for reference  $ref")
      }
    case msg                             => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}

object PrinterRegistryActor {

  implicit val printerDescriptionFormat = Json.format[PrinterDescription]
  implicit val printerDataFormat        = Json.format[PrinterData]

  def props: Props = Props[PrinterRegistryActor]
  case class PrinterData(id: Option[Int], settings: Option[PrinterDescription], status: Option[PrinterConnectionStatus] = None)
  case class PrinterDataList(printers: List[PrinterData])
  case class PrinterInstance(description: PrinterDescription,
                             connection: Option[ActorRef] = None,
                             status: Option[PrinterConnectionStatus] = None
                            ) {
    def withStatus(p: PrinterConnectionStatus) = copy(status = Some(p))

    def withConnection(c: ActorRef) = copy(connection = Some(c))


    def withDescription(d: PrinterDescription) = copy(description = d)
  }
  case class PrinterDescription(name: String = "unknown", config: Option[Configuration] = None)
  object PrinterDataList {
    def fromMap(printers: Map[Int, PrinterInstance]): PrinterDataList = {
      val list = printers.map {
        case (id, printer) =>
          PrinterData(Some(id), settings = Some(printer.description), status = printer.status)
      }(collection.breakOut): List[PrinterData]
      PrinterDataList(printers = list)
    }
  }

}
