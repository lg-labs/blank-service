package com.blanksystem.blank.service.domain.event;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.labs.lg.pentagon.common.domain.event.DomainEvent;

import java.time.ZonedDateTime;

public abstract class BlankEvent implements DomainEvent<Blank> {
    private final Blank blank;

    private final ZonedDateTime createdAt;

    protected BlankEvent(Blank blank, ZonedDateTime createdAt) {
        this.blank = blank;
        this.createdAt = createdAt;
    }

    public Blank getBlank() {
        return blank;
    }
}
