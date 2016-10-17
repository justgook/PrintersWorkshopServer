#gracefulStop
Feature: Actors must be immortal

  Background:
    Given Client-State connected to "/socket"

  Scenario: connectionRegistry Actor must be immortal and self restart even if got error
    When Actor with name "connection-registry" dies
    Then Actor with name "connection-registry" must be live

  Scenario: protocolEegistry Actor must be immortal and self restart even if got error
    When Actor with name "protocol-registry" dies
    Then Actor with name "protocol-registry" must be live

  Scenario: printersConnectionsRegistry Actor must be immortal and self restart even if got error
    When Actor with name "printers-connections-registry" dies
    Then Actor with name "printers-connections-registry" must be live

  Scenario: printersSettingsRegistry Actor must be immortal and self restart even if got error
    When Actor with name "printers-settings-registry" dies
    Then Actor with name "printers-settings-registry" must be live

  Scenario: fileRegistry Actor must be immortal and self restart even if got error
    When Actor with name "file-registry" dies
    Then Actor with name "file-registry" must be live
