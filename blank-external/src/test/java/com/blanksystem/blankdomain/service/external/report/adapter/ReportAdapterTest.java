package com.blanksystem.blankdomain.service.external.report.adapter;

import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.blanksystem.blankdomain.service.external.report.mapper.ReportDataMapper.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReportAdapterTest {

    @Test
    void it_should_create_report_from_blank_using_a_mapper() {
        // given
        final var blank = new Blank(new BlankId(UUID.randomUUID()));

        // when
        final var user = INSTANCE.blankToUser(blank);

        // then
        assertNotNull(user);
        assertEquals(blank.getId().getValue().toString(), user.userId());
    }
}