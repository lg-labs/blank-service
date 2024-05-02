package com.blanksystem.blank.service.domain.exception;

import com.labs.lg.pentagon.common.domain.exception.DomainException;

public class BlankApplicationServiceException extends DomainException {

    public BlankApplicationServiceException(String message) {
        super(message);
    }

    public BlankApplicationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
