/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package actors


import java.io.{File => JFile}
import java.nio.file.{StandardWatchEventKinds => EventType}

import actors.Subscribers.{AfterAdd, AfterTerminated}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.IO
import ch.jodersky.flow.Serial
import play.api.libs.json.{Json, OFormat}

import scala.language.postfixOps


class FileRegistryActor(directory: String) extends Actor with ActorLogging with Subscribers {

  //  val file: File = "/tmp" / "test.txt"
  //  file.overwrite("hello")
  //  file.appendLine().append("world")
  //  assert(file.contentAsString == "hello\nworld")
  //
  //  val myDir: File = File(directory)
  //  //  onEvent
  //  val watcher = new ThreadBackedFileMonitor(myDir, recursive = true) {
  //    override def onEvent(eventType: WatchEvent.Kind[Path], file: File): Unit = eventType match {
  //      case EventType.ENTRY_CREATE => println(s"$file got created")
  //      case EventType.ENTRY_MODIFY => println(s"$file got modified")
  //      case EventType.ENTRY_DELETE => println(s"$file got deleted")
  //    }
  //  }

  //
  //  watcher.start()
  //
  ////  FileWatcher
  ////  val watcher2: ActorRef = myDir.newWatcher(recursive = true)

  import actors.FileRegistryActor._
  import context._


  IO(Serial) ! Serial.Watch(directory)

  def receive(files: List[File], subscribers: Set[ActorRef]): Receive = {
    case AfterTerminated(oldSubscriber, newSubscribers)            =>
      context become subscribersParser(newSubscribers).orElse[Any, Unit](receive(files, newSubscribers))
    case AfterAdd(newSubscriber, newSubscribers)                   =>
      newSubscriber ! Files(files)
      context become subscribersParser(newSubscribers).orElse[Any, Unit](receive(files, newSubscribers))
    case Serial.CommandFailed(w: Serial.Watch, reason)             =>
    case Serial.Connected(file) if file matches s"$directory/\\d+" =>
      val newFiles = files.::(File(file, 0))
      log.warning(s"$subscribers")
      subscribers.foreach(c => c ! Files(newFiles))
      context become subscribersParser(subscribers).orElse[Any, Unit](receive(newFiles, subscribers))
    case msg                                                       => log.warning("Implement me, ({})", msg)
  }

  def receive: Receive = subscribersParser(Set.empty).orElse[Any, Unit](receive(List.empty, Set.empty))

  override def afterAdd(client: ActorRef, subscribers: Set[ActorRef]): Unit = {}

  override def afterTerminated(subscriber: ActorRef, subscribers: Set[ActorRef]): Unit = {}

}

object FileRegistryActor {
  //  def props: Props = Props[FileRegistryActor]
  def props(directory: String) = Props(new FileRegistryActor(directory))

  case class File(name: String, bytes: Int)

  case class Files(list: List[File])

  object File {
    implicit val fileFormat: OFormat[File] = Json.format[File]
  }

}
