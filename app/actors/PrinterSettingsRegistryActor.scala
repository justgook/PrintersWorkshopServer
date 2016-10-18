/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors


import actors.Subscribers.{AfterAdd, AfterTerminated}
import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props}
import akka.persistence._
import play.api.libs.json._
import protocols.{Configuration, StatusText}

/**
  * Created by Roman Potashow on 26.06.2016.
  */

class PrinterSettingsRegistryActor(printersConnections: ActorRef)
  extends PersistentActor with ActorLogging with Subscribers {

  import actors.PrinterSettingsRegistryActor._

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {}

  override def afterTerminated(subscriber: ActorRef, subscribers: Set[ActorRef]): Unit = {}


  def persistenceId: String = "PrinterSettingsRegistryActor"

  def restore(printers: Map[String, Printer]): Receive = {
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
      val newList = parsePrinterList(Printers(list), printers)
      log.info("Printer settings before become {}", newList)
      context become subscribersParser(Set.empty).orElse[Any, Unit](receive(newList, Set.empty))
    case RecoveryCompleted                               =>
      log.info("Printer settings restore restore completed")
    case s                                               => log.error("unknown msg {}", s)
  }

  def receive(printers: Map[String, Printer], subscribers: Set[ActorRef]): Receive = {
    case AfterTerminated(_, newSubscribers)      =>
      context become subscribersParser(newSubscribers).orElse[Any, Unit](receive(printers, newSubscribers))
    case AfterAdd(newSubscriber, newSubscribers) =>
      newSubscriber ! Printers(list = printers)
      context become subscribersParser(newSubscribers).orElse[Any, Unit](receive(printers, newSubscribers))
    case SaveSnapshotSuccess(metadata)           => log.info("SaveSnapshotSuccess, {}", metadata)
    case SaveSnapshotFailure(metadata, reason)   => log.info("SaveSnapshotFailure {}", reason)
    case p: Printers                             =>
      val newList = parsePrinterList(p, printers)
      log.info("Printer settings before become {}", newList)
      subscribers.foreach(c => c ! Printers(list = newList))
      context become subscribersParser(subscribers).orElse[Any, Unit](receive(newList, subscribers))
      saveState(newList)
      log.info("{}", printers.keys.toString())


    case (name: String, status: StatusText) =>
      var newList = printers
      printers.get(name) match {
        case Some(oPrinter: NewPrinter)       => newList += name -> NewPrinter(status)
        case Some(printer: ConfiguredPrinter) => newList += name -> printer.withStatus(status)
        case None                             => log.warning("Got status update for unknown printer")
      }
      subscribers.foreach(c => c ! Printers(list = newList))
      context become subscribersParser(subscribers).orElse[Any, Unit](receive(newList, subscribers))
    case t                                  => log.error("Got unknown message {}", t)

  }

  def receiveCommand: Receive = subscribersParser(Set.empty).orElse[Any, Unit](receive(Map.empty, Set.empty))

  def receiveRecover: Receive = restore(Map.empty)


  def parsePrinterList(newList: Printers, oldList: Map[String, Printer]): Map[String, Printer] = {
    var printers = oldList
    newList.list.foreach {
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
    printers
  }

  def saveState(printers: Map[String, Printer]): Unit = {
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
  def props(printersConnections: ActorRef) = Props(new PrinterSettingsRegistryActor(printersConnections))

  sealed trait Printer

  case class Printers(list: Map[String, Printer])

  case class NewPrinter(status: StatusText) extends Printer

  case class ConfiguredPrinter(status: StatusText, settings: Configuration) extends Printer {
    def withStatus(s: StatusText): ConfiguredPrinter = copy(status = s)
  }

  case class ExampleState(events: List[(String, JsValue)] = List.empty) {
    //    def updated(evt: Evt): ExampleState = copy(evt.data :: events)
    //    def size: Int = events.length
    override def toString: String = events.reverse.toString
  }

  object Printer {
    implicit val newPrinterFormat: OFormat[NewPrinter] = Json.format[NewPrinter]
    implicit val configuredPrinterFormat: OFormat[ConfiguredPrinter] = Json.format[ConfiguredPrinter]
    implicit val printerFormat = new Format[Printer] {
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
