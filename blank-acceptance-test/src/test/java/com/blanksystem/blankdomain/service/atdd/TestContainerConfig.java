package com.blanksystem.blankdomain.service.atdd;

import com.lg5.spring.testcontainer.container.AppCustomContainer;
import com.lg5.spring.testcontainer.container.DataBaseContainerCustomConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.time.Duration;
import java.util.Map;

import static com.lg5.spring.testcontainer.util.Constant.APP_PORT_DEFAULT;
import static com.lg5.spring.testcontainer.util.Constant.network;

public class TestContainerConfig extends DataBaseContainerCustomConfig {
    private static final Logger log = LoggerFactory.getLogger(TestContainerConfig.class);

    @Bean
    @Order(1)
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.3"))
                .withStartupTimeout(Duration.ofSeconds(30L))
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .waitingFor(Wait.forListeningPort())
                .withReuse(false);
        postgreSQLContainer.start();
        return postgreSQLContainer;
    }

    @Bean
    @Order(2)
    public KafkaContainer kafkaContainer() {
        KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .waitingFor(Wait.forListeningPort())
                .withReuse(false);
        kafkaContainer.start();
        return kafkaContainer;
    }

    @Bean
    @Order(3)
    public WireMockContainer wireMockContainer() {
        WireMockContainer wireMockContainer = new WireMockContainer("wiremock/wiremock:3.3.1")
                .withMappingFromResource("placeholder", "wiremock/placeholder/user-get.json")
                .withNetwork(network)
                .withNetworkAliases("mock-api")
                .waitingFor(Wait.forListeningPort())
                .withReuse(false);
        wireMockContainer.start();

        return wireMockContainer;
    }

    @Bean
    @Order(4)
    public GenericContainer<?> wireMockGuiContainer() {
        // Endpoint=> localhost:[dynamic_port]/__admin/webapp
        GenericContainer<?> wireMockGuiContainer = new GenericContainer<>(DockerImageName.parse("holomekc/wiremock-gui:3.6.31"))
                .withNetwork(network)
                .withExposedPorts(8080)
                .withNetworkAliases("mock-gui")
                //.withLogConsumer((OutputFrame outputFrame) -> log.info(outputFrame.getUtf8String()))
                .waitingFor(Wait.forListeningPort())
                .withAccessToHost(true)
                .withReuse(false);
        wireMockGuiContainer.start();
        return wireMockGuiContainer;
    }

    @Bean
    @Order(10)
    @DependsOn({"postgreSQLContainer", "kafkaContainer", "wireMockContainer", "wireMockGuiContainer"})
    public AppCustomContainer apiContainer(PostgreSQLContainer<?> postgreSQLContainer, KafkaContainer kafkaContainer, WireMockContainer wireMockContainer){
        AppCustomContainer appCustomContainer = new AppCustomContainer("com.blanksystem/blank-service:1.0.0-alpha");
        appCustomContainer.withFileSystemBind("./target/logs", "/logs", BindMode.READ_WRITE);
        appCustomContainer.withAppEnvVars(appWithEnvBuilder(postgreSQLContainer, kafkaContainer, wireMockContainer));
        // appCustomContainer.withLogConsumer((OutputFrame outputFrame) -> System.out.println(outputFrame.getUtf8String()));
        appCustomContainer.start();
        appCustomContainer.initRequestSpecification();
        return appCustomContainer;
    }

    private Map<String, String> appWithEnvBuilder(PostgreSQLContainer<?> postgreSQLContainer, KafkaContainer kafkaContainer, WireMockContainer wireMockContainer) {

        final String postgresUrl = String.format("jdbc:postgresql://postgres:%d/test", PostgreSQLContainer.POSTGRESQL_PORT);
        final String postgresUser = postgreSQLContainer.getUsername();
        final String postgresPassword = postgreSQLContainer.getPassword();

        final String kafkaBootstrapServers = "kafka:" + KafkaContainer.KAFKA_PORT + ",kafka:" + KafkaContainer.KAFKA_PORT + ",kafka:" + KafkaContainer.KAFKA_PORT;
        final String wireMockBaseUrl = wireMockContainer.getBaseUrl();


        Map<String, String> springDatasourceUrl = Map.of(
                "SERVER_PORT", String.valueOf(APP_PORT_DEFAULT),
                "SPRING_DATASOURCE_URL", postgresUrl,
                "SPRING_DATASOURCE_USERNAME", postgresUser,
                "SPRING_DATASOURCE_PASSWORD", postgresPassword,
                "KAFKACONFIG_BOOTSTRAPSERVERS", kafkaContainer.getBootstrapServers(),
                "KAFKA_BOOTSTRAP_SERVERS", kafkaContainer.getBootstrapServers(),
                "THIRD_JSONPLACEHOLDER_URL", wireMockBaseUrl,
                "log.path", "/logs"
        );

        springDatasourceUrl.forEach((s, s2) -> {
            System.out.println( s +","+ s2);
        });
        return springDatasourceUrl;
    }
/*
    @Bean
    @Order(10)
    @DependsOn({"postgreSQLContainer", "kafkaContainer", "wireMockContainer", "wireMockGuiContainer"})
    public GenericContainer<?> apiContainer(PostgreSQLContainer<?> postgreSQLContainer, KafkaContainer kafkaContainer, WireMockContainer wireMockContainer) {
        //appWithEnvBuilder(postgreSQLContainer, kafkaContainer, wireMockContainer);


        try {
            //     * "\${project.groupId}/\${project.parent.artifactId}:\${project.version}"
            System.out.println("LUISSSSS " + System.getProperty("blanksystem.blank.service.image.name"));
            GenericContainer<?> apiContainer = new GenericContainer<>(DockerImageName.parse("com.blanksystem/blank-service:1.0.0-alpha"))
                    .withExposedPorts(8080)
                    .withNetwork(network)
                    .withNetworkAliases("api")
                    .withEnv(appWithEnvBuilder(postgreSQLContainer, kafkaContainer, wireMockContainer))
                    .withFileSystemBind("./target/logs", "/logs", BindMode.READ_WRITE)

                    .withLogConsumer((OutputFrame outputFrame) -> log.info(outputFrame.getUtf8String()))
                    .waitingFor(Wait.forListeningPort())
                    .waitingFor(new HttpWaitStrategy()
                            .forPath("/actuator/health")
                            .forPort(8080)
                            .withStartupTimeout(Duration.ofSeconds(50)))

                    .withReuse(false);
            apiContainer.start();
            requestSpecification = (new RequestSpecBuilder()).setPort(apiContainer.getFirstMappedPort()).addHeader("Content-Type", "application/json").build();
            return apiContainer;
        } finally {

        }


    }*/


}
