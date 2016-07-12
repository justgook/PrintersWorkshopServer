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


  /*TODO get rid of ID - then there will no be need of store lastId, connection2id, and printers can me list,
   * but need find a way how to order them inside
   */
  private var state = State(Map.empty)

  override def afterAdd(client: ActorRef): Unit = {
    client ! PrinterDataList.fromMap(state.printers)
  }

  def receive = withSubscribers {
    case PrinterDataList(list) =>
      state = state.withDataListUpdate(PrinterDataList(list), context)
      subscribers.route(PrinterDataList.fromMap(state.printers), self)

    case status: PrinterConnectionStatus => //update Status form connection
      state = state.withStatusUpdate(status, sender())
      subscribers.route(PrinterDataList.fromMap(state.printers), self)

    case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
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
  private case class State(printers: Map[String, PrinterInstance]) {

    def withStatusUpdate(status: PrinterConnectionStatus, sender: ActorRef): State = {
      //      val ref = sender
      val result = printers map {
        case (id, PrinterInstance(desc, Some(conn), _))
          if conn == sender              => id -> PrinterInstance(desc, Some(conn), Some(status))
        case (id, item: PrinterInstance) => id -> item

      }
      State(result)
    }

    def withDataListUpdate(list: PrinterDataList, context: ActorContext): State = {
      val result = list.printers.map {
        case PrinterData(name, settings, _) => //Create new printer
          val printer =
            settings match {
              case Some(config) =>
                val ref = protocols.connect(config, context)
                context watch ref
                PrinterInstance(settings).withConnection(ref)
              case None         => PrinterInstance(settings)
            }
          name -> printer
      }(collection.breakOut): Map[String, PrinterInstance]
      State(printers = result)
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
