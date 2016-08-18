package actors


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.libs.json.Json

/**
  * Created by Roman Potashow on 03.08.2016.
  */
class FileRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.FileRegistryActor._

  def receive = withSubscribers {
    case msg => log.warning("Implement me")
  }

  override def afterAdd(subscriber: ActorRef) = subscriber ! Files(List.empty)

}

object FileRegistryActor {
  def props: Props = Props[FileRegistryActor]
  case class File(name: String, bytes: Int)
  case class Files(list: List[File])
  object File {
    implicit val fileFormat = Json.format[File]
  }
}
