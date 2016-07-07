package protocols

import play.api.libs.json._

/**
  * Created by Roman Potashow on 30.06.2016.
  */
sealed trait Property extends Product {
  type T
  lazy val `type`: String = productPrefix.split("-") match {
    case Array(_, x) => x
    case Array(x)    => x
    case _           => sys.error(s"illegal property $productPrefix")
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
  implicit val boolFormat         = Json.format[`bool`]
  implicit val stringFormat       = Json.format[`string`]
  implicit val intFormat          = Json.format[`int`]
  implicit val selectStringFormat = Json.format[`select-string`]
  implicit val selectIntFormat    = Json.format[`select-int`]

  implicit val writes = new Writes[Property] {
    def write[X: Writes](x: X) = implicitly[Writes[X]].writes(x)

    override def writes(o: Property): JsValue = {
      val body: JsValue = o match {
        case x: `bool`          => write(x)
        case x: `string`        => write(x)
        case x: `int`           => write(x)
        case x: `select-int`    => write(x)
        case x: `select-string` => write(x)
      }
      val tpe = Json.obj("type" -> o.`type`)
      val args: JsObject = body match {
        case value: JsObject => value
        case value           => Json.obj("args" -> value)
      }
      tpe ++ args
    }
  }
  implicit val reads  = new Reads[Property]() {
    override def reads(json: JsValue): JsResult[Property] = {
      def read[X: Reads]: JsResult[X] = json.validate[X]
      (json \ "type").validate[String] flatMap {
        case "bool"                                  => read[`bool`]
        case "int"
          if (json \ "enum").isInstanceOf[JsDefined] => read[`select-int`]
        case "int"                                   => read[`int`]
        case "string"
          if (json \ "enum").isInstanceOf[JsDefined] => read[`select-string`]
        case "string"                                => read[`string`]
        case x                                       => JsError(s"Unknown property type $x")
      }
    }
  }

  case class `bool`(name: String, label: String, defaultValue: Boolean) extends Property {
    type T = Boolean
  }
  case class `string`(name: String, label: String, defaultValue: String) extends Property {
    type T = String
  }
  case class `int`(name: String, label: String, defaultValue: Int) extends Property {
    type T = Int
  }
  case class `select-string`(name: String, label: String, defaultValue: Option[String] = None, enum: List[String]) extends EnumProperty {
    type E = String
  }
  case class `select-int`(name: String, label: String, defaultValue: Option[Int] = None, enum: List[Int]) extends EnumProperty {
    type E = Int
  }

}

//TODO rename to Schema...Builder
case class Settings(name: String, label: String, properties: List[Property])

object Settings {
  implicit val settingsFormat = Json.format[Settings]
}
