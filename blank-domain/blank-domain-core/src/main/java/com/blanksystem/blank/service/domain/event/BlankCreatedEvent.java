package com.blanksystem.blank.service.domain.event;

import com.blanksystem.blank.service.domain.entity.Blank;

import java.time.ZonedDateTime;

public class BlankCreatedEvent extends BlankEvent {

    public BlankCreatedEvent(Blank blank, ZonedDateTime createdAt) {
        super(blank, createdAt);
    }
}
