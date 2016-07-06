package protocols

import play.api.libs.json._

/**
  * Created by Roman Potashow on 30.06.2016.
  */

//TODO rename to Schema...Builder
case class Settings(name: String, label: String, properties: List[Settings.Property])

object Settings {


  implicit val stringPropertyReads        = Json.reads[StringProperty]
  implicit val inReads                    = new Reads[Property]() {
    override def reads(json: JsValue): JsResult[Property] = {
      def read[T: Reads] = implicitly[Reads[T]].reads((json \ "args").get)
      (json \ "type").as[String] match {
        //        case "bool"   => read[BoolProperty]
        case "string" => read[StringProperty]
      }
    }
  }
  implicit val boolPropertyWrites         = Json.writes[BoolProperty]
  implicit val stringPropertyWrites       = Json.writes[StringProperty]
  implicit val selectStringPropertyWrites = Json.writes[SelectStringProperty]
  implicit val intPropertyWrites          = Json.writes[IntProperty]
  implicit val selectIntPropertyWrites    = Json.writes[SelectIntProperty]
  implicit val propertyWrites             = new Writes[Property] {
    override def writes(o: Property): JsValue = {
      def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)
      val (t, data) = o match {
        case property: BoolProperty         => ("bool", Some(write(property)))
        case property: SelectStringProperty => ("string", Some(write(property)))
        case property: StringProperty       => ("string", Some(write(property)))
        case property: SelectIntProperty    => ("int", Some(write(property)))
        case property: IntProperty          => ("int", Some(write(property)))
      }
      //TODO find solution how output without sub-object args
      Json.obj("type" -> t) ++ {
        data.map(args => Json.obj("args" -> args)) getOrElse Json.obj()
      }
    }
  }
  implicit val settingsFormat             = Json.format[Settings]
  sealed trait Property
  case class BoolProperty(name: String, label: String, defaultValue: Boolean) extends Property
  case class StringProperty(name: String, label: String, defaultValue: String) extends Property
  case class SelectStringProperty(name: String, label: String, defaultValue: Option[String] = None, enum: List[String]) extends Property
  case class IntProperty(name: String, label: String, defaultValue: Int) extends Property
  case class SelectIntProperty(name: String, label: String, defaultValue: Option[Int] = None, enum: List[Int]) extends Property


}



