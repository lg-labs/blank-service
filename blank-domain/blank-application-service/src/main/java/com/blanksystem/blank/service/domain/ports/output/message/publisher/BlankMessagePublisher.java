package com.blanksystem.blank.service.domain.ports.output.message.publisher;


import com.blanksystem.blank.service.domain.event.BlankCreatedEvent;

public interface BlankMessagePublisher {
    void publish(BlankCreatedEvent blankCreatedEvent);
}
