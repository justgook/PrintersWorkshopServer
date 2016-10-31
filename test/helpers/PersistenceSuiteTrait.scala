/*
 * Copyright (c) PrinterWorkshopServer - 2016. - Roman Potashow
 */

package helpers

/**
  * Created by Roman Potashow on 22.08.2016.
  */

import com.typesafe.config.{Config, ConfigFactory}

object PersistenceSuiteTrait {
  val originalConfig: Config = ConfigFactory.load()
  def config(): Config ={
    val config = ConfigFactory.parseString(
      s"""
        akka.loglevel = OFF
        akka.stdout-loglevel = OFF
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
         """).withFallback(originalConfig)
    config
  }


  def journalId = "dummy-journal"

  def snapStoreId = "dummy-snapshot-store"
}
