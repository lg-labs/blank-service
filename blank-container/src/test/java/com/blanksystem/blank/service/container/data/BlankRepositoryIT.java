package com.blanksystem.blank.service.container.data;

import com.blanksystem.blank.service.boot.Bootstrap;
import com.blanksystem.blank.service.data.entity.BlankEntity;
import com.blanksystem.blank.service.data.repository.BlankJPARepository;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.BlankRepository;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


class BlankRepositoryIT extends Bootstrap {

    @Autowired
    BlankRepository blankRepository;

    @Autowired
    BlankJPARepository blankJPARepository;

    BlankRepositoryIT() {
        super();
    }

    @Test
    void it_should_save_a_blank_into_repository() {
        //given
        final var blank = new Blank(new BlankId(UUID.randomUUID()));

        // when
        final var blankReceived = blankRepository.createBlank(blank);

        // then
        assertNotNull(blankReceived);
        assertEquals(blankReceived, blank);
    }

    @Test
    void it_should_save_a_blank_into_repository2() {
        //given
        final BlankEntity blank = new BlankEntity(null);

        // when
        assertThrows(JpaSystemException.class, () -> blankJPARepository.save(blank));

    }
}
