package com.blanksystem.blank.service.domain;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.exception.BlankDomainException;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BlankTest {

    @Test
    void it_should_create_a_blank_successfully() {
        final Blank blank = new Blank(new BlankId(UUID.randomUUID()));

        assertDoesNotThrow(blank::validate);

    }

    @Test
    void it_should_throw_exception_when_blank_id_is_null() {
        final Blank blank = new Blank(null);

        assertThrows(BlankDomainException.class, blank::validate);

    }
}
