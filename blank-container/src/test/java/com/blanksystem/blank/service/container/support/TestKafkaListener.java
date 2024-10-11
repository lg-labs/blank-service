package com.blanksystem.blank.service.container.support;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TestKafkaListener {

    private String receivedMessage;

    @KafkaListener(
            id = "${blanksystem.blank.events.journal.blank.consumer.group}-test",
            topics = "${blanksystem.blank.events.journal.blank.topic}"
    )
    public void listen(String message) {
        receivedMessage = message;
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }
}
