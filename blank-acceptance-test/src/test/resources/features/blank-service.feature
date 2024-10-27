Feature:
  I as customer want create an blank using the repository template

  Background:
    Given a blank command

  @case1
  Scenario: the blank should be CREATED when use the repository template
    When blank is created
    Then the blank will be created using the repository template
    And the blank created event will be sent
    And the third system will be called to report the blank created

  @case2
  Scenario: the blank should be REPORTED when receive a blank created
    Given a blank stored
    When the blank created event is sent
    Then the blank created event will be sent
    And the third system will be called to report the blank created
