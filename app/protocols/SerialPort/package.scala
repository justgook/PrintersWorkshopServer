package protocols

import play.api.libs.json.Json
import protocols.Property._

/**
  * Created by Roman Potashow on 25.07.2016.
  */
package object SerialPort

  extends Protocol {
  val name     = "serialport"
  var settings = Settings(name = name, label = "Serial Port", properties = List(
    `select-string`(name = "port", label = "Device", enum = List("/dev/ttys004", "COM1", "COM2", "COM3", "COM4"), defaultValue = Some("/dev/ttys004")),
    `int`(name = "baud", label = "Baud rate", defaultValue = 115200),
    `int`(name = "cs", label = "Char size", defaultValue = 8),
    `bool`(name = "tsb", label = "Use two stop bits", defaultValue = false),
    `select-int`(name = "parity", label = "Parity (0=None, 1=Odd, 2=Even)", enum = List(0, 1, 2), defaultValue = Some(0))
  ))

  case class SerialPortConfiguration(port: String, baud: Int, cs: Int, tsb: Boolean, parity: Int) extends Configuration

  object SerialPortConfiguration {
    implicit val serialPortConfigurationFormat = Json.format[SerialPortConfiguration]
  }


}
