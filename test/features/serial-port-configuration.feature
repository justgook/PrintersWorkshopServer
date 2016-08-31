Feature: SerialPort Configuration

  Background:
    Given Client-State connected to "/socket"

  Scenario: receive info about printer status if create printer with serial-port configuration
    When Client-State create printer with name "Test Printer"
    And Update Printer "Test Printer" protocol to "serialport"
    Then Printer "Test Printer" status become "connected"

  Scenario: remove printer connection if connection changed to none
    When Client-State create printer with name "Test Printer"
    And Update Printer "Test Printer" protocol to "serialport"
    Then Printer "Test Printer" is connected
    When Update Printer "Test Printer" protocol to "none"
    Then Printer-Connection not have "Test Printer"

  Scenario: connect to serial port and open direct communication
    When Client-State create printer with name "123"
    And Update Printer "123" protocol to "serialport"
    Then Printer "123" is connected
    When socket connected to "/socket/123"
    Then Client-socket got text message "CONNECTED"
