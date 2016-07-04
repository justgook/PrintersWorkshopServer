package protocols

/**
  * Created by Roman Potashow on 30.06.2016.
  */
trait Protocol {
  def name: String

  def settings: Settings

  //  def connection(): AnyRef

}

object Protocol {
  case class SettingsUpdate(list: List[Settings])
}
