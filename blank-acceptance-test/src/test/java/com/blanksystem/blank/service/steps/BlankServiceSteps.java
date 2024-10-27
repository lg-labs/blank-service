package com.blanksystem.blank.service.steps;

import com.blanksystem.blank.service.support.data.BlankEntity;
import com.blanksystem.blank.service.support.data.BlankJPARepository;
import com.blanksystem.blank.service.support.dto.CreateBlankCommand;
import com.blanksystem.blank.service.support.world.BlankWorld;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.lg5.spring.testcontainer.container.AppCustomContainer.requestSpecification;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
@AllArgsConstructor
public class BlankServiceSteps {

    private final BlankWorld blankWorld;
    private final BlankJPARepository blankJPARepository;

    @Given("a blank command")
    public void aBlankCommand() {
        blankWorld.setCreateBlankCommand(new CreateBlankCommand(UUID.randomUUID()));
        log.info("The blank command with id: {}", blankWorld.getCreateBlankCommand().id());
    }

    @Given("a repository template")
    public void aRepositoryTemplate() {
        blankWorld.setCreateBlankCommand(new CreateBlankCommand(UUID.randomUUID()));
        log.info("The blank command already!!!");
    }

    @Given("a blank stored")
    public void aBlankStored() {
        log.info("a blank stored");
        blankJPARepository.save(new BlankEntity(blankWorld.getCreateBlankCommand().id()));
    }

    @When("blank is created")
    public void blankIsCreated() {
        final Response response = given(requestSpecification)
                .body(blankWorld.getCreateBlankCommand())
                .when()
                .post("/blank");
        blankWorld.setResponse(response);
        log.info("blank is created");
    }


    @Then("the blank will be created using the repository template")
    public void theBlankWillBeCreatedUsingTheRepositoryTemplate() {
        log.info("This is a blank will be created using the repository template");
        await()
                .atMost(5, SECONDS)
                .until(() -> blankWorld.getResponse().getStatusCode() == 202);
    }


}
