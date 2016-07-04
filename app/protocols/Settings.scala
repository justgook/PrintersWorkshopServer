package protocols

import play.api.libs.json._

/**
  * Created by Roman Potashow on 30.06.2016.
  */

//TODO rename to Schema...Builder
case class Settings(name: String, label: String, properties: List[Settings.Property])

object Settings {

  implicit val stringPropertyReads       = Json.reads[StringProperty]
  implicit val inReads                   = new Reads[Property]() {
    override def reads(json: JsValue): JsResult[Property] = {
      def read[T: Reads] = implicitly[Reads[T]].reads((json \ "args").get)
      (json \ "type").as[String] match {
        //        case "bool"   => read[BoolProperty]
        case "string" => read[StringProperty]
      }
    }
  }
  implicit val stringPropertyWrites      = Json.writes[StringProperty]
  implicit val multiStringPropertyWrites = Json.writes[MultiStringProperty]
  implicit val propertyWrites            = new Writes[Property] {
    override def writes(o: Property): JsValue = {
      def write[T: Writes](x: T) = implicitly[Writes[T]].writes(x)
      val (t, data) = o match {
        case _: BoolProperty               => ("bool", None)
        case property: MultiStringProperty => ("string", Some(write(property)))
        case property: StringProperty      => ("string", Some(write(property)))
      }
      //TODO find solution how output without sub-object args
      Json.obj("type" -> t) ++ {
        data.map(args => Json.obj("args" -> args)) getOrElse Json.obj()
      }
    }
  }
  implicit val settingsFormat            = Json.format[Settings]
  sealed trait Property
  case class BoolProperty() extends Property
  case class StringProperty(name: String, label: String, defaultValue: String) extends Property
  case class MultiStringProperty(name: String, label: String, defaultValue: Option[String] = None, enum: List[String]) extends Property

}



