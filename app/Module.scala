import actors.{ClientConnectionRegistryActor, PrinterRegistryActor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import protocols.ProtocolsRegistryActor

/**
  * Created by Roman Potashow on 25.07.2016.
  */
class Module extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor[ClientConnectionRegistryActor]("ws-connection-registry")
    bindActor[ProtocolsRegistryActor]("protocol-registry")
    bindActor[PrinterRegistryActor]("printers-registry")
  }
}
