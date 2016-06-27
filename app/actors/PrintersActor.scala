package actors


import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{BroadcastRoutingLogic, Router}

/**
  * Created by Roman Potashow on 26.06.2016.
  */
class PrintersActor extends Actor with ActorLogging {

  import actors.PrintersActor._


  private var list: List[ActorRef] = List()
  // TODO add way to save and store on restart

  private var subscribers = Router(BroadcastRoutingLogic())
  //  private var printers = Router(ConsistentHashingRoutingLogic())
  self ! CreateNewPrinter("s")

  override def receive = {
    //TODO update it to router with akka.routing.ConsistentHashingRoutingLogic
    case CreateNewPrinter(s) => list ::= context.actorOf(Printer.props(name = s))
      println(list)
      subscribers.route(PrintersListUpdate(list), self)

    case client: ActorRef =>
      context watch client
      subscribers = subscribers.removeRoutee(client)
      subscribers = subscribers.addRoutee(client)
      subscribers.route(PrintersListUpdate(list), self)

    case Terminated(client) =>
      subscribers = subscribers.removeRoutee(client)
  }
}

object PrintersActor {
  def props: Props = Props[PrintersActor]
  case class CreateNewPrinter(name: String)
  case class PrintersListUpdate(list: List[ActorRef])
}