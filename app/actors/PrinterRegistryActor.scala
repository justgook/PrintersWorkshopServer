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
  private var printers     : Map[Int, PrinterInstance] = Map()
  //TODO update it to router with akka.routing.ConsistentHashingRoutingLogic
  private var connection2id: Map[ActorRef, Int]        = Map()

  override def afterAdd(client: ActorRef): Unit = {
    client ! PrinterDataList.fromMap(printers)
  }

  def receive = withSubscribers {
    case PrinterDescription(name, None) => //Create new Printer From client or any other actor
      val settings = PrinterDescription(name) //TODO is way to parse all that instance from case
      lastId += 1
      printers += (lastId -> PrinterInstance(settings))
      Logger.info(s"CreatedPrinter $printers")
      subscribers.route(PrinterDataList.fromMap(printers), self)
    //      sender() ! PrinterDataList.fromMap(printers)
    //      sender() ! PrinterData(lastId, Some(settings))
    case PrinterDescription(name, Some(config)) => //Restore printer
      val settings = PrinterDescription(name, Some(config)) //TODO is way to parse all that instance from case
      lastId += 1
      val ref = protocols.connect(config, context)
      connection2id += ref -> lastId
      printers += (lastId -> PrinterInstance(description = settings, connection = Some(ref)))
      subscribers.route(PrinterDataList.fromMap(printers), self)
    //      sender() ! PrinterDataList.fromMap(printers)
    //      sender() ! PrinterData(lastId, Some(settings))

    case PrinterData(id, Some(settings), None) => // Update configuration from Client
      printers.get(id) match {
        case Some(printer) =>
          printers += (id -> printer.withDescription(settings))

          (settings, printer.description.config) match {
            case (PrinterDescription(name, Some(config)), Some(printerConfig)) if printerConfig != config =>
              Logger.error(s"NEED IMPLEMENT RECONNECT with new $config")
            case (PrinterDescription(name, Some(config)), None)                                           =>
              Logger.error(s"NEED IMPLEMENT CONNECT with new $config")
            case (PrinterDescription(name, Some(config)), Some(printerConfig)) if printerConfig == config =>
              Logger.error("IF configs is same no need to reconnect! maybe just ignore that case")
            case (PrinterDescription(name, None), printerConfig)                                          =>
              Logger.info("Just Update Name")
          }

          printer.connection match {
            case Some(connection) =>
              implicit val timeout = Timeout(1.seconds)
              //TODO replace with real message
              val future = connection ? "hello"
              val status = Await.result(future, timeout.duration).asInstanceOf[PrinterConnectionStatus]
              printers += (id -> printer.withStatus(status))
              subscribers.route(PrinterDataList.fromMap(printers), self)
            case None             =>
              subscribers.route(PrinterDataList.fromMap(printers), self)
          }
        case _             => Logger.info(s"${self.path.name}(${this.getClass.getName}) printer with id $id not found")
      }
    case status: PrinterConnectionStatus       => //update Status form connection
      val ref = sender()
      //TODO maybe there is better solution maybe destroy connection2id at all??
      connection2id.get(ref) match {
        case Some(id) => printers.get(id) match {
          case Some(printer) => printers += (id -> printer.withStatus(status))
          case None          => Logger.warn(s"${self.path.name}(${this.getClass.getName}) no printer with ID $id")
        }
        case None     => Logger.warn(s"${self.path.name}(${this.getClass.getName}) no id for reference  $ref")
      }
    case msg                                   => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}

object PrinterRegistryActor {

  implicit val printerDescriptionFormat = Json.format[PrinterDescription]
  implicit val printerDataFormat        = Json.format[PrinterData]

  def props: Props = Props[PrinterRegistryActor]
  case class PrinterData(id: Int, settings: Option[PrinterDescription], status: Option[PrinterConnectionStatus] = None)
  case class PrinterDataList(printers: List[PrinterData])
  case class PrinterInstance(description: PrinterDescription,
                             connection: Option[ActorRef] = None,
                             status: Option[PrinterConnectionStatus] = None
                            ) {
    def withStatus(p: PrinterConnectionStatus) = copy(status = Some(p))

    def withDescription(d: PrinterDescription) = copy(description = d)
  }
  case class PrinterDescription(name: String = "unknown", config: Option[Configuration] = None)
  object PrinterDataList {
    def fromMap(printers: Map[Int, PrinterInstance]): PrinterDataList = {
      val list = printers.map {
        case (a, printer) =>
          PrinterData(a, settings = Some(printer.description), status = printer.status)
      }(collection.breakOut): List[PrinterData]
      PrinterDataList(printers = list)
    }
  }

}
