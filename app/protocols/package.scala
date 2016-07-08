
import akka.actor.{ActorContext, ActorRef}
import protocols.demoport.DemoPort
import protocols.serialport.SerialPort

/**
  * Created by Roman Potashow on 30.06.2016.
  */
package object protocols {
  // TODO change to Actor that will react on Settings changes and put that list inside it as tuples of Optional[Watcher] + Optional[Setting]
  var settings = List(DemoPort.settings)

  def connect(config: Connection.Configuration, context: ActorContext): ActorRef = config match {
    case Connection.Configuration("serialport", _) =>
      context.actorOf(SerialPort.ConnectionActor.props(config = config))
    case Connection.Configuration("demoport", _)   =>
      context.actorOf(DemoPort.ConnectionActor.props(config = config))
    //    case msg => Logger.warn(s"connect(${this.getClass.getName}) unknown connection type '${config.name}'");
  }
}
