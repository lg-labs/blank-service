package com.blanksystem.blank.service.domain.exception;


import com.labs.lg.pentagon.common.domain.exception.DomainException;

public class BlankNotFoundException extends DomainException {

    public BlankNotFoundException(String message) {
        super(message);
    }
}
