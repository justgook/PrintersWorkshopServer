package protocols.serialport

import akka.actor.Props
import play.api.Logger
import protocols.Settings.MultiStringProperty
import protocols.{Connection, Protocol, Settings}

/**
  * Created by Roman Potashow on 30.06.2016.
  */

object SerialPort extends Protocol {
  val name     = "serialport"
  var settings = Settings(name = name, label = "Serial Port", properties = List(
    MultiStringProperty(name = "port", label = "port", enum = List("COM1", "COM2", "COM3", "COM4"))
  ))

  class SerialPortConnectionActor(config: Connection.Configuration) extends Connection {
    // TODO create subclass of that Connection.Configuration (SerialPort.Configuration)
    def receive = {
      case msg => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
    }
  }

  object SerialPortConnectionActor {
    def props(config: Connection.Configuration) = Props(new SerialPortConnectionActor(config))
  }

}
