
import akka.actor.{ActorContext, ActorRef}
import protocols.serialport.SerialPort

/**
  * Created by Roman Potashow on 30.06.2016.
  */
package object protocols {
  // TODO change to Actor that will react on Settings changes
  var settings = List(SerialPort.settings)


  //TODO is that good solution to use ActorContext to create new connection ?
  def connect(config: Connection.Configuration, parent: ActorContext): ActorRef = config match {
    case Connection.Configuration("serialport", args) =>
      parent.actorOf(SerialPort.SerialPortConnectionActor.props(config = config))
    //    case msg => Logger.warn(s"connect(${this.getClass.getName}) unknown connection type '${config.name}'");
  }
}
