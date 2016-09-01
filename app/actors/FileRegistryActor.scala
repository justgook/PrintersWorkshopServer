package actors


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.IO
import ch.jodersky.flow.Serial
import play.api.libs.json.Json

/**
  * Created by Roman Potashow on 03.08.2016.
  */
class FileRegistryActor(directory: String) extends Actor with ActorLogging with Subscribers {

  import actors.FileRegistryActor._
  import context._

  IO(Serial) ! Serial.Watch(directory)
  var files: List[File] = List.empty

  def receive = withSubscribers {
    case Serial.CommandFailed(w: Serial.Watch, reason)             =>
    case Serial.Connected(file) if file matches s"$directory/\\d+" => files ::= File(file, 0); log.info("got file {}", file)
    case msg                                                       => log.warning("Implement me, ({})", msg)
  }

  override def afterAdd(subscriber: ActorRef) = subscriber ! Files(files)

}

object FileRegistryActor {
  //  def props: Props = Props[FileRegistryActor]
  def props(directory: String) = Props(new FileRegistryActor(directory))

  case class File(name: String, bytes: Int)

  case class Files(list: List[File])

  object File {
    implicit val fileFormat = Json.format[File]
  }

}
