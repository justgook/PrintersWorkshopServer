package features.steps

/**
  * Created by Roman Potashow on 22.08.2016.
  */

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
//plugin = Array("pretty", "html:target/cucumber-report")
@CucumberOptions(
  features = Array("test/features"),
  glue = Array("features.steps"),
  plugin = Array("pretty")
  //  tags = Array("@wip")
)
class RunCucumber


