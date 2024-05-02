package com.blanksystem.blank.service.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.BaseId;

import java.util.UUID;

public class BlankId extends BaseId<UUID> {
    public BlankId(UUID value) {
        super(value);
    }
}
