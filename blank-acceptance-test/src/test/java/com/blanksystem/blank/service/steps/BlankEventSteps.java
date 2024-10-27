package com.blanksystem.blank.service.steps;

import com.blanksystem.blank.service.message.model.avro.BlankAvroModel;
import com.blanksystem.blank.service.support.journal.BlankEventKafkaPublisher;
import com.blanksystem.blank.service.support.world.BlankWorld;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
@AllArgsConstructor
public class BlankEventSteps {


    private final BlankWorld blankWorld;
    private final BlankEventKafkaPublisher publisher;

    @When("the blank created event is sent")
    public void theBlankCreatedEventIsSent() {
        publisher.publish(BlankAvroModel.newBuilder()
                .setId(blankWorld.getCreateBlankCommand().id().toString())
                .build());
    }

    @Then("the blank created event will be sent")
    public void theBlankCreatedEventWillBeSent() {
        await()
                .atMost(5, SECONDS)
                .until(() ->
                        blankWorld.getMessages().stream()
                                .anyMatch(blankAvroModel ->
                                        blankAvroModel.getId().equals(blankWorld.getCreateBlankCommand().id().toString())));
    }
}
