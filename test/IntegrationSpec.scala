import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._


import play.core.server.Server

//import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.json._
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * add your integration spec here.
  * An integration test will fire up a whole play application in a real (or headless) browser
  */
class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include("Your new application is ready.")
    }
  }


}
