package com.blanksystem.blank.service.container.api;

import com.blanksystem.blank.service.container.Bootstrap;
import com.blanksystem.blank.service.container.support.TestKafkaListener;
import com.blanksystem.blank.service.domain.dto.create.CreateBlankCommand;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.ReportRepository;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import io.restassured.filter.log.LogDetail;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;



class BlankCreatorIT extends Bootstrap {

    private static final String regex = "\\{\"id\": \"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\"\\}";

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    TestKafkaListener testKafkaListener;


    @Test
    void it_should_create_a_blank_using_api() {
        //given
        final var createBlankCommand = new CreateBlankCommand(UUID.randomUUID());

        // when
        final Response response = given(requestSpecification)
                .body(createBlankCommand)
                .when()
                .post("/blank");
        // then
        response.then().statusCode(HttpStatus.ACCEPTED.value());
        response.then().log().ifValidationFails(LogDetail.ALL);

        // Wait for the Kafka listener to receive the message using Awaitility
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(this::thenReceivedMessageSuccessfully);

    }

    private void thenReceivedMessageSuccessfully() {
        final String receivedMessage = testKafkaListener.getReceivedMessage();
        assertNotNull(receivedMessage);
        assertFalse(receivedMessage.isEmpty());
        final Matcher matcher = Pattern.compile(regex).matcher(receivedMessage);
        assertTrue(matcher.matches());
    }


    @Test
    void testWithThirdPartyApi() {
        reportRepository.save(new Blank(new BlankId(UUID.randomUUID())));
        assertTrue(true);
    }


}
