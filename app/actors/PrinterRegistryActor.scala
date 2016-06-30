package actors


import actors.PrinterActor.{PrinterStateUpdate, State => PrinterState}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.Logger

/**
  * Created by Roman Potashow on 26.06.2016.
  */
class PrinterRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.PrinterRegistryActor._

  // TODO add way to save and store on restart not printers pat Printers it self
  private var printers: Map[ActorRef, Option[PrinterState]] = Map()

  //  private var printers = Router(ConsistentHashingRoutingLogic())
  self ! CreateNewPrinter("slkjmytnhgfd")

  def receive = withSubscribers {
    //TODO update it to router with akka.routing.ConsistentHashingRoutingLogic
    case CreateNewPrinter(s)       => val ref = context.actorOf(PrinterActor.props(name = s)); printers = printers + (ref -> None)
    case PrinterStateUpdate(state) => printers = printers updated(sender(), Some(state))
    case msg                       => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  }
}

object PrinterRegistryActor {
  def props: Props = Props[PrinterRegistryActor]
  case class CreateNewPrinter(name: String)
  //TODO change to printerState update - send out all printers state
  case class PrintersListUpdate(list: List[PrinterState])
}
