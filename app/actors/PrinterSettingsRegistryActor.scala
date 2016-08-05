package actors


import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import play.api.libs.json._
import protocols.Connection.Configuration
import protocols.StatusText
import protocols.StatusText.StatusText

/**
  * Created by Roman Potashow on 26.06.2016.
  */
//TODO add way to save and store on restart Application
class PrinterSettingsRegistryActor @Inject()
(@Named("printers-connections") printersConnections: ActorRef) //TODO find way how move that part to Module.scala
  extends Actor with ActorLogging with Subscribers {

  import actors.PrinterSettingsRegistryActor._

  private var printers: Map[String, Printer] = Map.empty

  override def afterAdd(client: ActorRef) = client ! Printers(list = printers)

  def receive = withSubscribers {
    case Printers(list)                     =>
      list.foreach {
        case (name, printer: NewPrinter)        =>
          printers.get(name) match {
            case Some(oPrinter) if printer.status == StatusText.Remove =>
              printers -= name
            case Some(oPrinter: NewPrinter)                            =>
              log.info(s"Status update for printer $name")
              printers += name -> printer
            case Some(oPrinter: ConfiguredPrinter)                     =>
              log.info(s"downgrade ConfiguredPrinter -> NewPrinter $name")
              printers += name -> NewPrinter(StatusText.Unknown)
              printersConnections ! (name, PoisonPill)
            case None                                                  =>
              log.info(s"NewPrinter with name $name")
              printers += name -> printer
          }
        case (name, printer: ConfiguredPrinter) =>
          printers.get(name) match {
            case Some(oPrinter: NewPrinter)        =>
              log.info("upgrade NewPrinter -> ConfiguredPrinter")
              printersConnections ! (name, printer.settings)
              printers += name -> printer
            case Some(oPrinter: ConfiguredPrinter) =>
              log.info("ConfiguredPrinter change settings")
              if (printer.settings != oPrinter.settings)
                printersConnections ! (name, printer.settings)
              printers += name -> printer
            case None                              =>
              log.info(s"create new ConfiguredPrinter '$name'")
              printersConnections ! (name, printer.settings)
              printers += name -> printer
          }
      }
      subscribers.route(Printers(list = printers), self)
    case (name: String, status: StatusText) =>
      printers.get(name) match {
        case Some(oPrinter: NewPrinter)       => printers += name -> NewPrinter(status)
        case Some(printer: ConfiguredPrinter) => printers += name -> printer.withStatus(status)
        case None                             => log.warning("Got status update for unknown printer")
      }
      subscribers.route(Printers(list = printers), self)

    case t => log.warning(s"Got unknown message $t")
  }
}

object PrinterSettingsRegistryActor {

  def props: Props = Props[PrinterSettingsRegistryActor]
  sealed trait Printer
  case class Printers(list: Map[String, Printer])
  case class NewPrinter(status: StatusText) extends Printer
  case class ConfiguredPrinter(status: StatusText, settings: Configuration) extends Printer {
    def withStatus(s: StatusText) = copy(status = s)
  }

  object Printer {
    implicit val newPrinterFormat        = Json.format[NewPrinter]
    implicit val configuredPrinterFormat = Json.format[ConfiguredPrinter]
    implicit val printerFormat           = new Format[Printer] {
      //
      override def writes(o: Printer): JsValue = {
        o match {
          case printer: NewPrinter        => Json.obj("status" -> printer.status)
          case printer: ConfiguredPrinter => Json.obj("status" -> printer.status, "settings" -> printer.settings)
        }
      }

      override def reads(json: JsValue): JsResult[Printer] = {
        json \ "settings" \ "properties" match {
          case p: JsDefined        => json.validate[ConfiguredPrinter]
          case _                   => json.validate[NewPrinter]
        }

      }
    }
  }

}
