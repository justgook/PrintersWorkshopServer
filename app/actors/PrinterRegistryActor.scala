package actors


import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import play.api.Logger
import play.api.libs.json.Json
import protocols.Connection.{Configuration, Status => PrinterConnectionStatus}
/**
  * Created by Roman Potashow on 26.06.2016.
  */
//TODO add way to save and store on restart Application
class PrinterRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.PrinterRegistryActor._

  private var state = State(Map.empty)

  override def afterAdd(client: ActorRef) = client ! PrinterDataList.fromMap(state.printers)


  def receive = withSubscribers {
    case PrinterDataList(list) =>
      state = state.fromDataListUpdate(list, context)
      subscribers.route(PrinterDataList.fromMap(state.printers), self)

    case status: PrinterConnectionStatus => //update Status form connection
      state = state.withStatusUpdate(status, sender())
      subscribers.route(PrinterDataList.fromMap(state.printers), self)
  }
}

object PrinterRegistryActor {

  //  implicit val printerDescriptionFormat = Json.format[PrinterSettings]
  implicit val printerDataFormat = Json.format[PrinterData]

  def props: Props = Props[PrinterRegistryActor]
  case class PrinterData(name: String, settings: Option[Configuration] = None, status: Option[PrinterConnectionStatus] = None)
  case class PrinterDataList(printers: List[PrinterData])
  private case class PrinterInstance(settings: Option[Configuration] = None,
                                     connection: Option[ActorRef] = None,
                                     status: Option[PrinterConnectionStatus] = None
                                    ) {
    def withStatus(p: PrinterConnectionStatus) = copy(status = Some(p))

    def withConnection(c: ActorRef) = copy(connection = Some(c))

    def withSettings(c: Configuration) = copy(settings = Some(c))
  }
  //  private object  State {
  //    def getListDiff(list1: List[Any], list2: List[Any]) = {
  //      val unwanted = list2.toSet
  //      list1.filterNot(unwanted)
  //    }
  //  }
  private case class State(printers: Map[String, PrinterInstance]) {
    //    import State._
    //Status update from connection
    def withStatusUpdate(status: PrinterConnectionStatus, sender: ActorRef): State = {
      val result = printers map {
        case (name, PrinterInstance(desc, Some(conn), _))
          if conn == sender                => name -> PrinterInstance(desc, Some(conn)).withStatus(status)
        case (name, item: PrinterInstance) => name -> item
      }
      State(result)
    }

    def fromDataListUpdate(list: List[PrinterData], context: ActorContext): State = {
      val toUpdateOrCreate = list.filterNot(PrinterDataList.fromMap(printers).printers.toSet)
      //TODO clear it up and find some solution how to detect updates / creation / remove
      Logger.info("\n!!fromDataListUpdate!!\n")
      val result = list.map {
        case PrinterData(name, settings, None)         => //Create new printer
          val printer =
            settings match {
              case Some(config) =>
                val ref = protocols.connect(config, context)
                context watch ref
                PrinterInstance(settings).withConnection(ref)
              case None         => PrinterInstance(settings)
            }
          name -> printer
        case PrinterData(name, settings, Some(status)) => //Update printer
          name -> PrinterInstance(settings).withStatus(status)
      }(collection.breakOut): Map[String, PrinterInstance]

      State(result)
    }
  }
  object PrinterDataList {
    def fromMap(printers: Map[String, PrinterInstance]): PrinterDataList = {
      val list = printers.map {
        case (name, printer) =>
          PrinterData(name, settings = printer.settings, status = printer.status)
      }(collection.breakOut): List[PrinterData]
      PrinterDataList(printers = list)
    }
  }

}
