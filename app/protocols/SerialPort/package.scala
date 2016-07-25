package protocols

//import actors.PrinterRegistryActor.PrinterData
//import akka.actor.Props
//import play.api.Logger
import protocols.Property._

//import protocols.{Connection, Protocol, Settings}
/**
  * Created by Roman Potashow on 25.07.2016.
  */
package object SerialPort

  extends Protocol {
  val name     = "serialport"
  var settings = Settings(name = name, label = "Serial Port", properties = List(
    `select-string`(name = "port", label = "Device", enum = List("/dev/ttys003", "COM1", "COM2", "COM3", "COM4")),
    `int`(name = "baud", label = "Baud rate", defaultValue = 115200),
    `bool`(name = "tsb", label = "Use two stop bits", defaultValue = false),
    `select-int`(name = "parity", label = "Parity (0=None, 1=Odd, 2=Even)", enum = List(0, 1, 2))
  ))


  //  class ConnectionActor(config: Connection.Configuration) extends Connection {
  //    // TODO create subclass of that Connection.Configuration (SerialPort.Configuration)
  //    def receive = {
  //      case PrinterData(id, _, _) => Logger.info(s"SerialPortConnectionActor got id - $id, when my  $config");
  //      case msg                   => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
  //    }
  //  }

  //  object ConnectionActor {
  //    def props(config: Connection.Configuration) = Props(new ConnectionActor(config))
  //  }

}
