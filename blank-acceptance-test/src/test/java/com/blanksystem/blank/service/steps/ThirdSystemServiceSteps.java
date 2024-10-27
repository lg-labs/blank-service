package com.blanksystem.blank.service.steps;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.en.Then;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;

import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.shaded.com.github.dockerjava.core.MediaType.APPLICATION_JSON;

@Slf4j
@AllArgsConstructor
public class ThirdSystemServiceSteps {

    private static final String EXPECTED_URL = "/users";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final int EXPECTED_STATUS = 204;
    private static final long TIMEOUT_SECONDS = 5;

    @Then("the third system will be called to report the blank created")
    public void theThirdSystemWillBeCalledToReportTheBlankCreated() {
        await().during(1, MINUTES);
        // INFO: replace a sleep() with await().atMost(60, SECONDS).pollDelay(60, SECONDS);
        try {
            await()
                    .atMost(TIMEOUT_SECONDS, SECONDS)
                    .until(this::isThirdSystemInvokedSuccessfully);

            log.info("Third system successfully called to report the black creation.");
        } catch (ConditionTimeoutException e) {
            log.error("Timed out waiting for the third system to be called. Expected URL: {}, with status: {}.", EXPECTED_URL, EXPECTED_STATUS);
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred while checking third system invocation.", e);
            throw e;
        }


    }

    private boolean isThirdSystemInvokedSuccessfully() {
        log.debug("Checking WireMock serve events for third system invocation...");

        final List<ServeEvent> serveEvents = WireMock.getAllServeEvents();
        if (serveEvents.isEmpty()) {
            log.warn("No serve events captured for third system calls.");
        }

        return serveEvents.stream().anyMatch(this::isServeEventValid);
    }

    private boolean isServeEventValid(ServeEvent serveEvent) {
        boolean isUrlValid = serveEvent.getRequest().getUrl().equals(EXPECTED_URL);
        boolean isContentTypeValid = APPLICATION_JSON.getMediaType().equals(serveEvent.getRequest().getHeader(CONTENT_TYPE_HEADER));
        boolean isStatusValid = serveEvent.getResponse().getStatus() == EXPECTED_STATUS;

        log.debug("ServeEvent validation -> URL valid: {}, Content-Type valid: {}, Status valid: {}", isUrlValid, isContentTypeValid, isStatusValid);

        return isUrlValid && isContentTypeValid && isStatusValid;
    }

}
