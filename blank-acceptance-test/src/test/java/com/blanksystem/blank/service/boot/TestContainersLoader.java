package com.blanksystem.blank.service.boot;

import com.lg5.spring.testcontainer.config.KafkaContainerCustomConfig;
import com.lg5.spring.testcontainer.config.PostgresContainerCustomConfig;
import com.lg5.spring.testcontainer.config.WireMockGuiContainerCustomConfig;
import com.lg5.spring.testcontainer.config.WiremockContainerCustomConfig;
import com.lg5.spring.testcontainer.container.AppCustomContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.Map;

import static com.lg5.spring.testcontainer.config.DataBaseContainerCustomConfig.JDBC_URL_CUSTOM;
import static com.lg5.spring.testcontainer.config.KafkaContainerCustomConfig.BOOTSTRAP_SERVERS_CUSTOM;
import static com.lg5.spring.testcontainer.util.Constant.APP_PORT_DEFAULT;

@Import({
        PostgresContainerCustomConfig.class,
        KafkaContainerCustomConfig.class,
        WiremockContainerCustomConfig.class,
        WireMockGuiContainerCustomConfig.class
})
public class TestContainersLoader {

    @Bean
    public AppCustomContainer apiContainer(PostgreSQLContainer<?> postgresContainer,
                                           KafkaContainer kafkaContainer,
                                           WireMockContainer wireMockContainer) {

        AppCustomContainer appCustomContainer = new AppCustomContainer("com.blanksystem/blank-service:1.0.0-alpha");
        appCustomContainer.withFileSystemBind("./target/logs", "/logs", BindMode.READ_WRITE);
        appCustomContainer.withAppEnvVars(appWithEnvBuilder(postgresContainer, kafkaContainer, wireMockContainer));
        // appCustomContainer.withLogConsumer((OutputFrame outputFrame) -> System.out.println(outputFrame.getUtf8String()));
        appCustomContainer.start();
        appCustomContainer.initRequestSpecification();
        return appCustomContainer;
    }

    private Map<String, String> appWithEnvBuilder(PostgreSQLContainer<?> postgreSQLContainer,
                                                  KafkaContainer kafkaContainer,
                                                  WireMockContainer wireMockContainer) {


        return Map.of(
                "SERVER_PORT", String.valueOf(APP_PORT_DEFAULT),
                "SPRING_DATASOURCE_URL", postgreSQLContainer.getEnvMap().get(JDBC_URL_CUSTOM),
                "SPRING_DATASOURCE_USERNAME", postgreSQLContainer.getUsername(),
                "SPRING_DATASOURCE_PASSWORD", postgreSQLContainer.getPassword(),
                "KAFKA-CONFIG_BOOTSTRAP-SERVERS", kafkaContainer.getEnvMap().get(BOOTSTRAP_SERVERS_CUSTOM),
                "THIRD_JSONPLACEHOLDER_URL", wireMockContainer.getBaseUrl(),
                "log.path", "/logs"
        );
    }

}
