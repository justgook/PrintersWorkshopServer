package protocols

import play.api.libs.json._

/**
  * Created by Roman Potashow on 30.06.2016.
  */
sealed trait Property extends Product {
  type T
  lazy val `type`: String = productPrefix.split("-") match {
    case Array(_, x) => x
    case Array(x) => x
    case _ => sys.error(s"illegal property $productPrefix")
  }
  def name: String
  def label: String
  def defaultValue: T
}

sealed trait EnumProperty extends Property {
  type E
  type T = Option[E]
  def enum: List[E]
}

object Property {
  case class `bool`         (name: String, label: String, defaultValue: Boolean) extends Property { type T = Boolean }
  case class `string`       (name: String, label: String, defaultValue: String) extends Property { type T = String }
  case class `int`          (name: String, label: String, defaultValue: Int) extends Property { type T = Int }
  case class `select-string`(name: String, label: String, defaultValue: Option[String] = None, enum: List[String]) extends EnumProperty { type E = String }
  case class `select-int`   (name: String, label: String, defaultValue: Option[Int] = None, enum: List[Int]) extends EnumProperty { type E = Int }
}

//TODO rename to Schema...Builder
case class Settings(name: String, label: String, properties: List[Property])
