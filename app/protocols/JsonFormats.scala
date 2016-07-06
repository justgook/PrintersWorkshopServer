package protocols

import play.api.libs.json._

object JsonFormats {

  object PropertyJson {
    import Property._

    private implicit val boolFormat = Json.format[`bool`]
    private implicit val stringFormat = Json.format[`string`]
    private implicit val intFormat = Json.format[`int`]
    private implicit val selectStringFormat = Json.format[`select-string`]
    private implicit val selectIntFormat = Json.format[`select-int`]

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
          case body: JsObject => body
          case body           => Json.obj("args" -> body)
        }
        tpe ++ args
      }
    }

    implicit val reads = new Reads[Property]() {
      override def reads(json: JsValue): JsResult[Property] = {
        def read[X: Reads]: JsResult[X] = (json \ "args").validate[X]
        (json \ "type").validate[String] flatMap {
          case "bool"   => read[`bool`]
          case "int"    => read[`int`]
          case "string" => read[`string`]
          // ...
          case x => JsError(s"Unknown property type $x")
        }
      }
    }
  }

  object SettingsJson {
    import PropertyJson._
    implicit val settingsFormat = Json.format[Settings]
  }

}
