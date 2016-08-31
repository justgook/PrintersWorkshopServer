Feature: WebSocket communication protocol

  Client-socket expects that server will communicate in json
  and will response with json.path updates and revision updates,
  abd will fail (close connection) if something wrong

  Background:
    Given socket connected to "/socket"

  Scenario: Connecting to /socket and receive pong
    When Client-socket send "Ping"
    Then Client-socket got "Pong"

  Scenario: get initial state, as `set` command
    Then Client-socket got "Set"

  Scenario: get fail, if send not defined type-command
    When Client-socket send text '{"type":"undefined-command"}'
    Then Client-socket got "Fail"

  Scenario: close connection, if send not json (or wrong formatted)
    When Client-socket send text 'not json'
    Then Client-socket got "Disconnected"

  Scenario: get full state, as `set` command after sending `reset`
    And initial state received
    When Client-socket send "Reset"
    Then Client-socket got "Set"

  Scenario: get fail message if try send update with wrong revision number
    And initial state received
    When Client-socket send text '{"type":"update", "revision": 9999, "args":[]}'
    Then Client-socket got "Fail"

  Scenario: get fail message if try send update inapplicable to state
    And initial state received
    When Client-socket send text '{"type":"update", "revision": 2, "args":[{"op":"remove","path":"/non-existing"}]}'
    Then Client-socket got "Fail"
