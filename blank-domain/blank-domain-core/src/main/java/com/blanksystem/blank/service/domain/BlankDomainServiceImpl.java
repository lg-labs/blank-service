package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.event.BlankCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.labs.lg.pentagon.common.domain.DomainConstants.UTC;

public class BlankDomainServiceImpl implements BlankDomainService {

    private static final Logger LOG = LoggerFactory.getLogger(BlankDomainServiceImpl.class);

    @Override
    public BlankCreatedEvent validateAndInitiateBlank(Blank blank) {
        //Any Business logic required for a process the blank creation
        blank.validate();
        LOG.info("Blank with id: {} is initiated", blank.getId().getValue());
        return new BlankCreatedEvent(blank, ZonedDateTime.now(ZoneId.of(UTC)));
    }
}
