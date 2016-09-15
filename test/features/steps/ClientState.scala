/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package features.steps

import java.net.URI

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import cucumber.api.scala.{EN, ScalaDsl}
import helpers.WebSocketClient.Messages.{Connected, Connecting}
import helpers.{StateObserver, WebSocketClient}
import play.api.libs.json._

//import play.api.libs.json.JsObject

/**
  * Created by Roman Potashow on 26.08.2016.
  */
trait ClientState extends ScalaDsl with EN {
  var client: ActorRef = _
  var `state-probe`: TestProbe = _
  var `state-socket`: WebSocketClient = _

  //  var clientStateSocket: WebSocketClient = _
  implicit def system: ActorSystem

  def port: Int

  def sendUpdate(args: JsArray) = {
    client ! (args, `state-socket`)
    awaitSuccess()
  }

  def awaitSuccess(): Unit = {
    `state-probe`.fishForMessage(hint = s"success not received") {
      case ("success", rev) => true
      case _                => false
    }
  }

  Given("""^Client-State connected to "([^"]*)"$""") { (url: String) =>
    `state-probe` = TestProbe()
    client = system.actorOf(StateObserver.props(`state-probe`))
    `state-socket` = WebSocketClient(new URI(s"ws://localhost:$port$url"), client)
    `state-socket`.connect()
    `state-probe`.expectMsg(Connecting)
    `state-probe`.expectMsg(Connected)
    `state-probe`.fishForMessage(hint = "state not got") {
      case (state: JsObject, rev) => rev == 1
      case _                      => false
    }
    super.After(s => `state-socket`.disconnect())
  }

  When("""^Client-State create printer with name "([^"]*)"$""") { (name: String) =>
    sendUpdate(Json.parse(s"""[{"op":"add","path":"/printers/$name","value":{"status":"unknown"}}]""").as[JsArray])
  }

  When("""^Update Printer "([^"]*)" protocol to "([^"]*)"$""") { (name: String, protocol: String) =>

    protocol match {
      case "none"        => sendUpdate(Json.parse(s"""[{"op":"add","path":"/printers/$name/settings","value":{"name":"$protocol"}}]""").as[JsArray])
      case "demoport"    => sendUpdate(Json.parse(s"""[{"op":"add","path":"/printers/$name/settings","value":{"name":"$protocol","properties":{"demo-select-string":"a"}}}]""").as[JsArray])
      case "setrialport" => sendUpdate(Json.parse(s"""[{"op":"add","path":"/printers/$name/settings","value":{"name":"$protocol","properties":{"demo-select-string":"a"}}}]""").as[JsArray])
      case _             => sendUpdate(Json.parse(s"""[{"op":"add","path":"/printers/$name/settings","value":{"name":"$protocol","properties":{"port":"/tmp/a","baud":115200, "cs":8,"tsb":false, "parity":0}}}]""").as[JsArray])
    }

  }
  Then("""^Printer-Connection have "([^"]*)"$""") { (name: String) =>
    `state-probe`.fishForMessage(hint = "printers status not got") {
      case (state: JsObject, rev) =>
        (state \ "conditions" \ name).isInstanceOf[JsDefined]
      case _                      => false
    }
  }

  Then("""^Printer-Connection not have "([^"]*)"$""") { (name: String) =>
    `state-probe`.fishForMessage(hint = "printers status not got") {
      case (state: JsObject, rev) =>
        (state \ "conditions" \ name).isInstanceOf[JsUndefined]
      case _                      => false
    }
  }
  Then("""^Printer "([^"]*)" is connected$""") { (name: String) =>
    `state-probe`.fishForMessage(hint = "printers status not got") {
      case (state: JsObject, rev) =>
        (state \ "conditions" \ name).isInstanceOf[JsDefined] && (state \ "conditions" \ name).isInstanceOf[JsDefined] && (state \ "printers" \ name \ "status").asOpt[String].contains("connected")
      case _                      => false
    }
  }


  Then("""^Printer "([^"]*)" status become "([^"]*)"$""") { (name: String, status: String) =>
    `state-probe`.fishForMessage(hint = "printers status not got") {
      case (state: JsObject, rev) => (state \ "printers" \ name \ "status").asOpt[String].contains(status)
      case _                      => false
    }
  }

  When("""^Printer "([^"]*)" status set "([^"]*)"$""") { (name: String, value: String) =>
    sendUpdate(Json.parse(s"""[{"op":"replace","path":"/printers/$name/status","value":"$value"}]""").as[JsArray])
  }
  Then("""^Printer "([^"]*)" not exist in settings list$""") { (name: String) =>
    `state-probe`.fishForMessage(hint = "printers status not got") {
      case (state: JsObject, rev) =>
        (state \ "printers" \ name).isInstanceOf[JsUndefined]
      case _                      => false
    }
  }

}
