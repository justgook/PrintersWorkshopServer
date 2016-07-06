package protocols.demoport

import actors.PrinterRegistryActor.PrinterData
import akka.actor.Props
import play.api.Logger
import protocols.Property._
import protocols.{Connection, Protocol, Settings}

/**
  * Created by Roman Potashow on 30.06.2016.
  */

object DemoPort extends Protocol {
  val name     = "demoport"
  var settings = Settings(name = name, label = "Demo Connection", properties = List(
    `bool`(name = "sdCard", label = "Have SD card", defaultValue = false),
    `int`(name = "defaultSpeed", label = "Printing Speed", defaultValue = 10)
  ))

  class ConnectionActor(config: Connection.Configuration) extends Connection {
    // TODO create subclass of that Connection.Configuration (SerialPort.Configuration)
    def receive = {
      case PrinterData(id, _, _) => Logger.info(s"demoport got id - $id, when my  $config");
      case msg                   => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
    }
  }

  object ConnectionActor {
    def props(config: Connection.Configuration) = Props(new ConnectionActor(config))
  }

}
