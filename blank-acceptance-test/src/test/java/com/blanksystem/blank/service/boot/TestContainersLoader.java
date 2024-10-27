package com.blanksystem.blank.service.boot;

import com.lg5.spring.kafka.config.data.KafkaConfigData;
import com.lg5.spring.testcontainer.config.AppContainerCustomConfig;
import com.lg5.spring.testcontainer.config.ContainerConfig;
import com.lg5.spring.testcontainer.config.KafkaContainerCustomConfig;
import com.lg5.spring.testcontainer.config.PostgresContainerCustomConfig;
import com.lg5.spring.testcontainer.config.WiremockContainerCustomConfig;
import com.lg5.spring.testcontainer.container.AppCustomContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Import({
        PostgresContainerCustomConfig.class,
        KafkaContainerCustomConfig.class,
        WiremockContainerCustomConfig.class,
        AppContainerCustomConfig.class
})
public final class TestContainersLoader {

    private final KafkaConfigData kafkaConfigData;

    private final List<ContainerConfig> containerConfigs;

    public TestContainersLoader(KafkaConfigData kafkaConfigData, List<ContainerConfig> containerConfigs) {
        this.kafkaConfigData = kafkaConfigData;
        this.containerConfigs = containerConfigs;
    }

    @Bean
    public AppCustomContainer apiContainer(AppCustomContainer appCustomContainer,
                                           PostgreSQLContainer<?> postgresContainer,
                                           KafkaContainer kafkaContainer,
                                           WireMockContainer wireMockContainer,
                                           GenericContainer<?> schemaRegistryContainer) {

        appWithEnvBuilder(appCustomContainer.getEnvMap(), postgresContainer, kafkaContainer,
                wireMockContainer, schemaRegistryContainer);

        appCustomContainer.start();
        appCustomContainer.initRequestSpecification();
        updateKafkaConfigData(kafkaContainer);

        return appCustomContainer;
    }

    private void updateKafkaConfigData(KafkaContainer kafkaContainer) {
        kafkaConfigData.setBootstrapServers(kafkaContainer.getBootstrapServers());
    }

    private void appWithEnvBuilder(Map<String, String> envMap, PostgreSQLContainer<?> postgreSQLContainer,
                                   KafkaContainer kafkaContainer,
                                   WireMockContainer wireMockContainer,
                                   GenericContainer<?> schemaRegistryContainer) {

        final Map<Class<?>, Consumer<Map<String, String>>> configActions = new HashMap<>();


        addPostgresConfig(postgreSQLContainer, configActions);


        addWiremockConfig(wireMockContainer, configActions);

        addKafkaConfig(kafkaContainer, schemaRegistryContainer, configActions);

        configActions.forEach((configClass, action) -> action.accept(envMap));


    }

    private void addKafkaConfig(KafkaContainer kafkaContainer, GenericContainer<?> schemaRegistryContainer, Map<Class<?>, Consumer<Map<String, String>>> configActions) {
        configActions.put(KafkaContainerCustomConfig.class,
                map -> containerConfigs.stream()
                        .filter(KafkaContainerCustomConfig.class::isInstance)
                        .findFirst()
                        .ifPresent(config -> map.putAll(((KafkaContainerCustomConfig) config)
                                .initializeEnvVariables(kafkaContainer, schemaRegistryContainer))));
    }

    private void addWiremockConfig(WireMockContainer wireMockContainer, Map<Class<?>, Consumer<Map<String, String>>> configActions) {
        configActions.put(WiremockContainerCustomConfig.class,
                map -> containerConfigs.stream()
                        .filter(WiremockContainerCustomConfig.class::isInstance)
                        .findFirst()
                        .ifPresent(config -> map.putAll(config.initializeEnvVariables(wireMockContainer))));
    }

    private void addPostgresConfig(PostgreSQLContainer<?> postgreSQLContainer, Map<Class<?>, Consumer<Map<String, String>>> configActions) {
        configActions.put(PostgresContainerCustomConfig.class,
                map -> containerConfigs.stream()
                        .filter(PostgresContainerCustomConfig.class::isInstance)
                        .findFirst()
                        .ifPresent(config -> map.putAll(config.initializeEnvVariables(postgreSQLContainer))));
    }

}
