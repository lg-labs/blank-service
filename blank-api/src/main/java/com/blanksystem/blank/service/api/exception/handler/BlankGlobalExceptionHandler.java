package com.blanksystem.blank.service.api.exception.handler;

import com.blanksystem.blank.service.domain.exception.BlankDomainException;
import com.blanksystem.blank.service.domain.exception.BlankNotFoundException;
import com.lg5.spring.api.rest.ErrorDTO;
import com.lg5.spring.api.rest.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <h1>Caught the Order Domain Layer errors</h1>
 */
@Slf4j
@ControllerAdvice
public class BlankGlobalExceptionHandler extends GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = {BlankDomainException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleException(BlankDomainException orderDomainException) {
        log.error(orderDomainException.getMessage(), orderDomainException);
        return new ErrorDTO(HttpStatus.BAD_REQUEST.getReasonPhrase(), orderDomainException.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = {BlankNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleException(BlankNotFoundException orderNotFoundException) {
        log.error(orderNotFoundException.getMessage(), orderNotFoundException);
        return new ErrorDTO(HttpStatus.NOT_FOUND.getReasonPhrase(), orderNotFoundException.getMessage());
    }


}
