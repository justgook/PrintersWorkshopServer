/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

import akka.actor.{ActorContext, ActorRef}
import play.api.libs.json._
import protocols.SerialPort._
import protocols.demoport.DemoPort
import protocols.demoport.DemoPort.DemoPortConfiguration


/**
  * Created by Roman Potashow on 30.06.2016.
  */

package object protocols {

  implicit val configurationFormat = new Format[Configuration] {

    override def writes(o: Configuration): JsValue = {
      def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)
      val (name: String, props) = o match {
        case config: SerialPortConfiguration => ("serialport", Some(write(config)))
        case config: DemoPortConfiguration   => ("demoport", Some(write(config)))
      }
      Json.obj("name" -> name) ++ {
        props.map(data => Json.obj("properties" -> data)) getOrElse Json.obj("properties" -> Json.obj())
      }
    }


    override def reads(json: JsValue): JsResult[Configuration] = {
      def read[T: Reads] = implicitly[Reads[T]].reads((json \ "properties").get)
      (json \ "name").asOpt[String] match {
        case Some("serialport") => read[SerialPortConfiguration]
        case Some("demoport")   => read[DemoPortConfiguration]
        case _            => JsError()
      }

    }

  }
  // TODO change to Actor that will react on Settings changes and put that list inside it as tuples of Optional[Watcher] + Optional[Setting]
  var settings = List(DemoPort.settings, SerialPort.settings)

  def connect(config: Configuration, context: ActorContext): ActorRef = config match {
    case config: SerialPortConfiguration =>
      context.actorOf(SerialPort.ConnectionActor.props(config = config))
    case config: DemoPortConfiguration   =>
      context.actorOf(DemoPort.ConnectionActor.props(config = config))
    //    case msg => Logger.warn(s"connect(${this.getClass.getName}) unknown connection type '${config.name}'");
  }
  trait Configuration

}
