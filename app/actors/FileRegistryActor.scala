package actors


import akka.actor.{Actor, ActorLogging, ActorRef, Props}

/**
  * Created by Roman Potashow on 03.08.2016.
  */
class FileRegistryActor extends Actor with ActorLogging with Subscribers {

  import actors.FileRegistryActor._

  def receive = {
    case msg => log.warning("Implement me")
  }

  override def afterAdd(subscriber: ActorRef): Unit = {
    subscriber ! Files(List.empty)
  }
}

object FileRegistryActor {
  def props: Props = Props[FileRegistryActor]
  case class File(name: String, bytes: Int)
  case class Files(list: List[File])
}
