package protocols.SerialPort

import akka.actor.{ActorRef, Props, Terminated}
import akka.io.IO
import akka.util.ByteString
import ch.jodersky.flow.{Parity, Serial, SerialSettings}
import protocols.Connection
import protocols.Connection.Status

import scala.util.Try

/**
  * Created by Roman Potashow on 25.07.2016.
  */
class ConnectionActor(config: Connection.Configuration) extends Connection {

  import ConnectionActor._
  import context._

  val port = config.properties.getOrElse("port", "/dev/master")
  //.orElse()
  //  parent ! status
  val aSettings: SerialSettings = (for {
    baudString <- config.properties.get("baud")
    baud <- Try(baudString.toInt).toOption
    csString <- config.properties.get("cs")
    cs <- Try(csString.toInt).toOption
    tsbString <- config.properties.get("tsb")
    tsb <- Try(tsbString.toBoolean).toOption
    parityString <- config.properties.get("parity")
    parity <- Try(parityString.toInt).toOption
  } yield {
    SerialSettings(baud, cs, tsb, Parity(parity))
  }).getOrElse(SerialSettings(9600))
  var status                    = Status()


  parent ! Connection.Configuration(
    name = config.name,
    properties = Map(
      "port" -> port,
      "baud" -> aSettings.baud.toString,
      "cs" -> aSettings.characterSize.toString,
      "tsb" -> aSettings.twoStopBits.toString,
      "parity" -> aSettings.parity.toString
    )
  )



  log.info(s"Requesting manager to open port: $port, baud: ${aSettings.baud}")
  //  context.parent ! status.withText(StatusText.Connecting)
  IO(Serial) ! Serial.Open(port, aSettings)

  def receive = {
    case Serial.CommandFailed(cmd, reason) =>
      log.error(s"Connection failed, stopping terminal. Reason: $reason")
      context stop self

    case Serial.Opened(p) =>
      log.debug(s"Port $p is now open.")
      val operator = sender
      context become opened(operator)
      context watch operator
    //      context.parent ! status.withText(StatusText.Connected)
  }

  def opened(operator: ActorRef): Receive = {

    case Serial.Received(data) =>
      log.info(s"Received data: ${formatData(data)}")

    case Terminal.Wrote(data) => log.info(s"Wrote data: ${formatData(data)}")

    case Serial.Closed =>
      log.info("Operator closed normally, exiting terminal.")
      context unwatch operator
      context stop self

    case Terminated(`operator`) =>
      log.error("Operator crashed, exiting terminal.")
      context stop self

    case EOT =>
      log.info("Initiating close.")
      operator ! Serial.Close


    case ConsoleInput(input) =>
      val data = ByteString(input.getBytes)
      operator ! Serial.Write(data, length => Wrote(data.take(length)))
    //          reader ! ConsoleReader.Read

  }

}
object ConnectionActor {
  def props(config: Connection.Configuration) = Props(new ConnectionActor(config))

  private def formatData(data: ByteString) = data.mkString("[", ",", "]") + " " + new String(data.toArray, "UTF-8")
  case class Wrote(data: ByteString) extends Serial.Event

  case class ConsoleInput(in: String)
  case object Read
  case object EOT
}
