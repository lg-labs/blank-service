package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.event.BlankCreatedEvent;

public interface BlankDomainService {
    BlankCreatedEvent validateAndInitiateBlank(Blank blank);
}
