package protocols.serialport

import actors.PrinterRegistryActor.PrinterData
import akka.actor.Props
import play.api.Logger
import protocols.Property._
import protocols.{Connection, Protocol, Settings}

/**
  * Created by Roman Potashow on 30.06.2016.
  */

object SerialPort extends Protocol {
  val name     = "serialport"
  var settings = Settings(name = name, label = "Serial Port", properties = List(
    `select-string`(name = "port", label = "port", enum = List("COM1", "COM2", "COM3", "COM4"))
  ))

  class ConnectionActor(config: Connection.Configuration) extends Connection {
    // TODO create subclass of that Connection.Configuration (SerialPort.Configuration)
    def receive = {
      case PrinterData(id, _, _) => Logger.info(s"SerialPortConnectionActor got id - $id, when my  $config");
      case msg                   => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
    }
  }

  object ConnectionActor {
    def props(config: Connection.Configuration) = Props(new ConnectionActor(config))
  }

}
