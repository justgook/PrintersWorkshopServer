/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

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
  case class SettingsList(list: List[Settings])
}
