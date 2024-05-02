Feature:
  I as customer want create an blank using the repository template

  Scenario: the blank should be CREATED when use the repository template
    Given a repository template
    When blank is created
    Then the blank will be created using the repository template