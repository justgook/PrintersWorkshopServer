package actors


import javax.inject.{Inject, Named}

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props}
import akka.persistence._
import play.api.libs.json._
import protocols.StatusText.StatusText
import protocols.{Configuration, StatusText}

/**
  * Created by Roman Potashow on 26.06.2016.
  */
//TODO add way to save and store on restart Application
class PrinterSettingsRegistryActor @Inject()
(@Named("printers-connections") printersConnections: ActorRef) //TODO find way how move that part to Module.scala
  extends PersistentActor with ActorLogging with Subscribers {

  import actors.PrinterSettingsRegistryActor._

  private var printers: Map[String, Printer] = Map.empty

  def persistenceId: String = "PrinterSettingsRegistryActor"

  override def afterAdd(client: ActorRef) = client ! Printers(list = printers)

  def receiveRecover = {

    case SnapshotOffer(metadata, snapshot: ExampleState) =>
      val list = (for (
        item <- snapshot.events;
        c = item._2.validate[Configuration]
      ) yield {
        c match {
          case JsSuccess(config, path) => item._1 -> ConfiguredPrinter(status = StatusText.Unknown, config)
          case JsError(eror)           => item._1 -> NewPrinter(status = StatusText.Unknown)
        }
      }).toMap[String, Printer]
      parsePrinterList(Printers(list))

    case RecoveryCompleted => log.info("Printer settings restore restore completed - {}", printers)
    case s                 => log.error("unknown msg {}", s)
  }

  def parsePrinterList(p: Printers): Unit = {
    p.list.foreach {
      case (name, printer: NewPrinter)        =>
        printers.get(name) match {
          case Some(oPrinter) if printer.status == StatusText.Remove =>
            printers -= name
          case Some(oPrinter: NewPrinter)                            =>
            log.info("Status update for printer {}", name)
            printers += name -> printer
          case Some(oPrinter: ConfiguredPrinter)                     =>
            log.info("downgrade ConfiguredPrinter -> NewPrinter {}", name)
            printers += name -> NewPrinter(StatusText.Unknown)
            printersConnections ! (name, PoisonPill)
          case None                                                  =>
            log.info("NewPrinter with name {}", name)
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
            log.info("create new ConfiguredPrinter '{}'", name)
            printersConnections ! (name, printer.settings)
            printers += name -> printer
        }
    }
  }

  def receiveCommand = withSubscribers {
    case SaveSnapshotSuccess(metadata)         => log.info("SaveSnapshotSuccess, {}", metadata)
    case SaveSnapshotFailure(metadata, reason) => log.info("SaveSnapshotFailure {}", reason)
    case p: Printers                           =>
      parsePrinterList(p)
      saveState()
      log.info("{}", printers.keys.toString())
      subscribers.route(Printers(list = printers), self)
    case (name: String, status: StatusText)    =>
      printers.get(name) match {
        case Some(oPrinter: NewPrinter)       => printers += name -> NewPrinter(status)
        case Some(printer: ConfiguredPrinter) => printers += name -> printer.withStatus(status)
        case None                             => log.warning("Got status update for unknown printer")
      }
      subscribers.route(Printers(list = printers), self)
    case t                                     => log.error("Got unknown message {}", t)
  }

  def saveState(): Unit = {
    val list: List[(String, JsValue)] = printers.map { case (name, value) =>
      value match {
        case p: NewPrinter        => (name, Json.obj())
        case p: ConfiguredPrinter => (name, Json.toJson(p.settings))
      }
    }.toList
    saveSnapshot(ExampleState(list))
    //    deleteSnapshots(SnapshotSelectionCriteria())
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

  case class ExampleState(events: List[(String, JsValue)] = List.empty) {
    //    def updated(evt: Evt): ExampleState = copy(evt.data :: events)
    //    def size: Int = events.length
    override def toString: String = events.reverse.toString
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
          case p: JsDefined => json.validate[ConfiguredPrinter]
          case _            => json.validate[NewPrinter]
        }

      }
    }
  }

}
