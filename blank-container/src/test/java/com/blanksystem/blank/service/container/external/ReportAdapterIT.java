package com.blanksystem.blank.service.container.external;

import com.blanksystem.blank.service.container.Bootstrap;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.ReportRepository;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import com.blanksystem.blankdomain.service.external.report.mapper.ReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;



class ReportAdapterIT extends Bootstrap {

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    ReportMapper reportMapper;

    @BeforeEach
    void setUp() {
        configureFor("localhost", 7070);
    }

    @Test
    void it_should_create_an_user_using_wiremock_for_a_third_party_api() {
        // given
        Blank blank = new Blank(new BlankId(UUID.randomUUID()));
        String requestBody = "{\"userId\": \"" + blank.getId().getValue() + "\"}";

        // when
        reportRepository.save(blank);

        // then
        verify(postRequestedFor(urlEqualTo("/users")));
        verify(postRequestedFor(urlEqualTo("/users"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(requestBody))
        );
    }
}