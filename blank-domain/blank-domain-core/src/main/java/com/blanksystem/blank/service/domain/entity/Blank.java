package com.blanksystem.blank.service.domain.entity;

import com.blanksystem.blank.service.domain.exception.BlankDomainException;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import com.labs.lg.pentagon.common.domain.entity.AggregateRoot;

public class Blank extends AggregateRoot<BlankId> {


    public Blank(BlankId blankId) {
        super.setId(blankId);
    }

    /**
     * My Business Logic
     */
    public void validate() {
        if (getId() == null) {
            throw new BlankDomainException("The Blank to create is invalid");
        }
    }

}
