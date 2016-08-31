Feature: Printer Configuration

  Background:
    Given Client-State connected to "/socket"

  Scenario: Creating new printer
    When Client-State create printer with name "Test Printer"
#    Then Printer count is 1

  Scenario: delete printer is status text set to "remove"
    When Client-State create printer with name "Test Printer"
    And Printer "Test Printer" status set "remove"
    Then Printer "Test Printer" not exist in settings list

  Scenario: receive info about printer status if create printer with connection configuration
    When Client-State create printer with name "Test Printer"
    And Update Printer "Test Printer" protocol to "demoport"
    Then Printer "Test Printer" status become "connected"

  Scenario: remove printer connection if connection changed to none
    When Client-State create printer with name "Test Printer"
    And Update Printer "Test Printer" protocol to "demoport"
    Then Printer "Test Printer" is connected
    When Update Printer "Test Printer" protocol to "none"
    Then Printer-Connection not have "Test Printer"

