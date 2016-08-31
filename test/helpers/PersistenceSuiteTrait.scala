package helpers

/**
  * Created by Roman Potashow on 22.08.2016.
  */

import com.typesafe.config.ConfigFactory

//import akka.testkit.TestKit
//import akka.testkit.ImplicitSender
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.Matchers
//import org.scalatest.WordSpecLike

object PersistenceSuiteTrait {

  def config() = ConfigFactory.parseString(
    s"""akka.loggers = [akka.testkit.TestEventListener] # makes both log-snooping and logging work
         akka.loglevel = "DEBUG"
         play.akka.loglevel = DEBUG
         akka.persistence.journal.plugin = "$journalId"
         akka.persistence.snapshot-store.plugin = "$snapStoreId"
         $journalId {
           class = "org.dmonix.akka.persistence.JournalPlugin"
           plugin-dispatcher = "akka.actor.default-dispatcher"
         }
        $snapStoreId {
          class = "org.dmonix.akka.persistence.SnapshotStorePlugin"
          plugin-dispatcher = "akka.persistence.dispatchers.default-plugin-dispatcher"
         }
         akka.actor.debug.receive = on""")

  def journalId = "dummy-journal"

  def snapStoreId = "dummy-snapshot-store"
}
