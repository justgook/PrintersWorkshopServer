package actors


import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, PoisonPill, Props}
import play.api.libs.json.Json
import protocols.Connection.{Configuration, Status => PrinterConnectionStatus}
import protocols.StatusText

/**
  * Created by Roman Potashow on 26.06.2016.
  */
//TODO add way to save and store on restart Application
class PrinterRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.PrinterRegistryActor._

  private var state = State(Map.empty)

  override def afterAdd(client: ActorRef) = client ! PrinterDataList.fromMap(state.printers)


  def receive = withSubscribers {
    case PrinterDataList(list)           =>
      state = state.fromDataListUpdate(list, context)
      subscribers.route(PrinterDataList.fromMap(state.printers), self)
    case status: PrinterConnectionStatus => //update Status form connection
      state = state.withStatusUpdate(status, sender())
      subscribers.route(PrinterDataList.fromMap(state.printers), self)
    case config: Configuration           => // Config update from serialPort
      state = state.withConfigUpdate(config, sender())
      subscribers.route(PrinterDataList.fromMap(state.printers), self)
    case t                               => log.warning(s"Got unknown message $t")
  }
}

object PrinterRegistryActor {

  //  implicit val printerDescriptionFormat = Json.format[PrinterSettings]
  implicit val printerDataFormat = Json.format[PrinterData]

  def props: Props = Props[PrinterRegistryActor]
  case class PrinterData(name: String, settings: Option[Configuration] = None, status: Option[PrinterConnectionStatus] = None)
  case class PrinterDataList(printers: List[PrinterData])
  private case class PrinterInstance(settings: Configuration = Configuration(),
                                     connection: Option[ActorRef] = None,
                                     status: Option[PrinterConnectionStatus] = None
                                    ) {
    def withStatus(p: Option[PrinterConnectionStatus]) = copy(status = p)

    def withStatus(p: PrinterConnectionStatus) = copy(status = Some(p))

    def withSettingsUpdate(context: ActorContext, c: Configuration) = {
      if (settings != c) {
        connection match {
          case Some(conn) => conn ! PoisonPill
          case None       =>
        }
        val ref = protocols.connect(c, context)
        context watch ref
        copy(settings = c).withConnection(ref)
      }
      else this
    }

    def withConnection(c: ActorRef) = copy(connection = Some(c))
  }

  private case class State(printers: Map[String, PrinterInstance]) {

    def withConfigUpdate(config: Configuration, sender: ActorRef): State = {
      val result = printers map {
        case (name, PrinterInstance(desc, Some(conn), status))
          if conn == sender                => name -> PrinterInstance(config, Some(conn), status)
        case (name, item: PrinterInstance) => name -> item
      }
      State(result)
    }

    def withStatusUpdate(status: PrinterConnectionStatus, sender: ActorRef): State = {
      val result = printers map {
        case (name, PrinterInstance(desc, Some(conn), _))
          if conn == sender                => name -> PrinterInstance(desc, Some(conn)).withStatus(Some(status))
        case (name, item: PrinterInstance) => name -> item
      }
      State(result)
    }

    def fromDataListUpdate(list: List[PrinterData], context: ActorContext): State = {
      val result = printers ++ list.map(item => {
        val name = item.name
        (item, printers.get(name)) match {
          case (newItem, None)          =>
            name -> createNewPrinter(context, newItem)
          case (newItem, Some(oldItem)) =>
            name -> updatePrinter(context, oldItem, newItem)
        }
      }).toMap[String, PrinterInstance]
      State(result)
    }

    private def createNewPrinter(context: ActorContext, newItem: PrinterData): PrinterInstance = {
      newItem.settings match {
        case Some(settings) => PrinterInstance().withSettingsUpdate(context, settings).withStatus(newItem.status)
        case None           => PrinterInstance().withStatus(newItem.status)
      }


    }


    private def updatePrinter(context: ActorContext, oldPrinter: PrinterInstance, newItem: PrinterData): PrinterInstance = {
      (oldPrinter, newItem) match {
        case (_, PrinterData(_, newSettings, newStatus)) =>
          var printer = oldPrinter
          if (newStatus.isDefined) {
            if (newStatus.get.text == StatusText.Connecting) {
              printer = printer.withStatus(printer.status.get.withText(StatusText.Connected))
            } else if (newStatus.get.text != StatusText.Editing && printer.connection.isDefined && printer.status != newStatus) {
              printer.connection.get ! newStatus.get
            } else printer = printer.withStatus(newStatus)
          }

          if (newSettings.isDefined && !newSettings.contains(oldPrinter.settings)) {
            printer = printer.withSettingsUpdate(context, newSettings.get)
          }
          printer
        case _                                           =>
          oldPrinter
      }
    }
  }
  object PrinterDataList {
    def fromMap(printers: Map[String, PrinterInstance]): PrinterDataList = {
      val list = printers.map {
        case (name, printer) =>
          PrinterData(name, settings = Some(printer.settings), status = printer.status)
      }(collection.breakOut): List[PrinterData]
      PrinterDataList(printers = list)
    }
  }

}
