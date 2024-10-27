package com.blanksystem.blank.service.container.external;

import com.blanksystem.blank.service.boot.Bootstrap;
import com.blanksystem.blank.service.domain.entity.Blank;
import com.blanksystem.blank.service.domain.ports.output.repository.ReportRepository;
import com.blanksystem.blank.service.domain.valueobject.BlankId;
import com.blanksystem.blankdomain.service.external.report.mapper.ReportDataMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*@TestPropertySource(properties = {
        //"testcontainers.kafka.enabled=false"
})*/
class ReportAdapterIT extends Bootstrap {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportDataMapper reportDataMapper;

    @Autowired
    private WireMockContainer wireMockContainer;

    private RequestSpecification specification;

    @BeforeEach
    void setUp() {
        this.specification = (new RequestSpecBuilder()).setPort(wireMockContainer.getPort())
                .addHeader("Content-Type", "application/json")
                .build();
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

    @Test
    void it_should_validate_a_wiremock_stub_to_user_api() {

        Blank blank = new Blank(new BlankId(UUID.randomUUID()));
        String uniqueTestId = UUID.randomUUID().toString();  // Generamos un testId único para cada prueba
        String requestBody = "{\"userId\": \"" + blank.getId().getValue() + "\", \"testId\": \"" + uniqueTestId + "\"}";

        stubFor(post(urlEqualTo("/users"))
                .withRequestBody(containing("\"testId\": \"" + uniqueTestId + "\""))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"result\": \"user created\", \"testId\": \"" + uniqueTestId + "\" }")));

        RequestSpecification givenARequest = given()
                .spec(specification)
                .body(requestBody);

        // when
        Response invokedThirdApi = givenARequest
                .when()
                .post("/users");

        // then
        invokedThirdApi
                .then()
                .statusCode(202);
        String responseBody = invokedThirdApi.body().asString();
        assertEquals("{ \"result\": \"user created\", \"testId\": \"" + uniqueTestId + "\" }", responseBody);  // Verificamos que el cuerpo contiene el testId


        // Verificamos que se hizo exactamente una solicitud POST a /users con el testId correcto
        verify(1, postRequestedFor(urlEqualTo("/users"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"testId\": \"" + uniqueTestId + "\""))
        );

    }
}