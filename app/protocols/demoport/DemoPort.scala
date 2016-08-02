package protocols.demoport

import akka.actor.Props
import play.api.Logger
import protocols.Connection.{Progress, Status, Temperature}
import protocols.Property._
import protocols.{Connection, Protocol, Settings}

/**
  * Created by Roman Potashow on 30.06.2016.
  */

object DemoPort extends Protocol {
  val name     = "demoport"
  var settings = Settings(name = name, label = "Demo Connection", properties = List(
    //    `int`(name = "connection-time", label = "How fast will connect", defaultValue = 0),
    //    `int`(name = "printing-speed", label = "Printing speed (delay between lines)", defaultValue = 0),
    //    `bool`(name = "demo-bool", label = "Switcher", defaultValue = false),
    //    `int`(name = "demo-int", label = "Some Number", defaultValue = 10),
    //    `select-int`(name = "demo-select-int", label = "Select Number", enum = List(1, 2, 3, 4, 5, 6, 7, 8, 9), defaultValue = Some(2)),
    //    `string`(name = "demo-string", label = "Enter Text Here", defaultValue = ""),
    `select-string`(name = "demo-select-string", label = "Select letter", enum = List("a", "b"), defaultValue = Some("b"))
  ))

  class ConnectionActor(config: Connection.Configuration) extends Connection {
    val r      = scala.util.Random
    var status = Status(
      progress = Some(Progress(done = 10, of = 300)),
      temperatures = Some(List(Temperature(data = List(
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        100 + r.nextInt(110),
        r.nextInt(210)
      ))))
    )

    context.parent ! status

    def receive = {
      //      case PrinterData(n, _, _)                          => Logger.info(s"demoport got name - $n, when my $config")
      //      case Status(_, Some(file), progress, temperatures) => //TODO add validation that printer-status not in printing state
      //        status = status.withFile(file).readyToPrint()
      //        context.parent ! status
      case msg                                           => Logger.warn(s"${self.path.name}(${this.getClass.getName}) unknown message received '$msg'")
    }
  }

  object ConnectionActor {
    def props(config: Connection.Configuration) = Props(new ConnectionActor(config))
  }

}
