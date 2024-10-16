package com.blanksystem.blank.service.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;


public class GivenStep {

    @Given("a repository template")
    public void aRepositoryTemplate() {
        System.out.println("This is a repository template");
    }

    @When("blank is created")
    public void blankIsCreated() {
    }

    @Then("the blank will be created using the repository template")
    public void theBlankWillBeCreatedUsingTheRepositoryTemplate() {
        System.out.println("This is a blank will be created using the repository template");
    }
}
