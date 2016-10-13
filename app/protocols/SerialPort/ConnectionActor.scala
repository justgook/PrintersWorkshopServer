/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package protocols.SerialPort

import actors.Subscribers2.{AfterAdd, AfterTerminated}
import akka.actor.{ActorRef, Props, Terminated}
import akka.io.IO
import akka.util.ByteString
import ch.jodersky.flow.{Parity, Serial, SerialSettings}
import protocols.Connection
import protocols.Connection.{ConsoleInput, Status}

/**
  * Created by Roman Potashow on 25.07.2016.
  */
class ConnectionActor(config: SerialPortConfiguration) extends Connection {

  import ConnectionActor._
  import context._

  val port: String = config.port

  val aSettings: SerialSettings = SerialSettings(config.baud, config.cs, config.tsb, Parity(config.parity))
  var status = Status()

  log.info(s"Requesting manager to open port: $port, baud: ${aSettings.baud}")
  //  context.parent ! status.withText(StatusText.Connecting)
  IO(Serial) ! Serial.Open(port, aSettings)


  //  override def afterAdd(client: ActorRef): Unit = {
  //    log.info("got direct connection")
  //    subscribers.route("CONNECTED", self)
  //  }

  //  override def afterTerminated(subscriber: ActorRef): Unit = {
  //    subscribers.route(ConnectionCountUpdate(subscribers.routees.size), self)
  //  }

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {}

  override def afterTerminated(subscriber: ActorRef, subscribers: Set[ActorRef]): Unit = {}

  def receive: Receive = subscribersParser(Set.empty).orElse[Any, Unit](opening(Set.empty))

  def opening(subscribers: Set[ActorRef]): Receive = {
    case AfterTerminated(_, newSubscribers)      =>
      context become subscribersParser(newSubscribers).orElse[Any, Unit](opening(subscribers))
    case AfterAdd(newSubscriber, newSubscribers) =>
      log.info("got direct connection")
      newSubscriber ! "CONNECTED"
      context become subscribersParser(newSubscribers).orElse[Any, Unit](opening(newSubscribers))
    case Serial.CommandFailed(cmd, reason)       =>
      log.error(s"Connection failed, stopping terminal. Reason: $reason")
      context stop self
    case Serial.Opened(p)                        =>
      log.info(s"Port $p is now open.")
      val operator = sender
      context become subscribersParser(subscribers).orElse[Any, Unit](opened(operator, subscribers))
      context watch operator
      context.parent ! status
  }


  def opened(operator: ActorRef, subscribers: Set[ActorRef]): Receive = {

    case AfterTerminated(_, newSubscribers)      =>
      context become subscribersParser(newSubscribers).orElse[Any, Unit](opened(operator, subscribers))
    case AfterAdd(newSubscriber, newSubscribers) =>
      log.info("got direct connection")
      newSubscriber ! "CONNECTED"
      context become subscribersParser(newSubscribers).orElse[Any, Unit](opened(operator, newSubscribers))

    case Serial.Received(data) =>
      log.info(s"Received data: ${formatData(data)}")
      subscribers.foreach(c => c ! new String(data.toArray, "UTF-8"))
    //    case Terminal.Wrote(data) => log.info(s"Wrote data: ${formatData(data)}")

    case Serial.Closed =>
      log.info("Operator closed normally, exiting terminal.")
      context unwatch operator
      context stop self

    case Terminated(`operator`) =>
      log.error("Operator crashed, exiting terminal.")
      context stop self

    //    case EOT =>
    //      log.info("Initiating close.")
    //      operator ! Serial.Close

    case ConsoleInput(input) =>
      val data = ByteString(input.getBytes)
      operator ! Serial.Write(data, length => Wrote(data.take(length)))

  }

}

object ConnectionActor {
  def props(config: SerialPortConfiguration) = Props(new ConnectionActor(config))

  private def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + new String(data.toArray, "UTF-8")

  case class Wrote(data: ByteString) extends Serial.Event

  //  case object EOT
}
