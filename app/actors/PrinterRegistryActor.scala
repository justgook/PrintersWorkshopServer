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

  implicit val printerDescriptionFormat = Json.format[PrinterDescription]
  implicit val printerDataFormat        = Json.format[PrinterData]

  def props: Props = Props[PrinterRegistryActor]
  case class PrinterData(id: Option[Int], settings: Option[PrinterDescription], status: Option[PrinterConnectionStatus] = None)
  case class PrinterDataList(printers: List[PrinterData])
  case class PrinterDescription(name: String = "unknown", config: Option[Configuration] = None) {
    def withName(n: String) = copy(name = n)

    def withConfig(c: Configuration) = copy(config = Some(c))
  }
  private case class PrinterInstance(description: PrinterDescription,
                                     connection: Option[ActorRef] = None,
                                     status: Option[PrinterConnectionStatus] = None
                                    ) {
    def withStatus(p: PrinterConnectionStatus) = copy(status = Some(p))

    def withConnection(c: ActorRef) = copy(connection = Some(c))

    def withDescription(d: PrinterDescription) = copy(description = d)
  }
  private case class State(printers: Map[Int, PrinterInstance]) {
    private var lastId = 0

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
        case PrinterData(None, Some(settings), _)     => //Create new printer
          lastId += 1
          val printer =
            settings.config match {
              case Some(config) =>
                val ref = protocols.connect(config, context)
                PrinterInstance(settings).withConnection(ref)
              case None         => PrinterInstance(settings)
            }
          lastId -> printer
        case PrinterData(Some(id), Some(settings), _) =>
          val printer =
            settings.config match {
              case Some(config) =>
                val ref = protocols.connect(config, context)
                PrinterInstance(settings).withConnection(ref)
              case None         => PrinterInstance(settings)
            }
          id -> printer

        //        case _                                    => 1 -> PrinterInstance(PrinterDescription("1"))
      }(collection.breakOut): Map[Int, PrinterInstance]
      State(printers = result)
    }
  }


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
