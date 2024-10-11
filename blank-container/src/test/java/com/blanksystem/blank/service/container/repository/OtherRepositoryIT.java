package com.blanksystem.blank.service.container.repository;

import com.blanksystem.blank.service.container.Bootstrap;
import com.blanksystem.blank.service.domain.ports.output.repository.OtherRepository;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtherRepositoryIT extends Bootstrap {

    @Autowired
    OtherRepository reportRepository;

    RequestSpecification givenRequestSpecification;

    @BeforeEach
    void setUp() {
        givenRequestSpecification = given(requestSpecification);

    }

    @Test
    void testWithThirdPartyApi() {
        reportRepository.search("");
        assertTrue(true);
    }
}
