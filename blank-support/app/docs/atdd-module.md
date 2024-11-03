# 🧪Acceptance Test(ATDD) Module

!!! Success "Good news for you 🎉"
    Finally, Lg5 supports Cucumber integration with Spring Boot 3, Testcontainers, and reuses many configurations for testing. This way, you can create acceptance tests for your domain services quickly.
    >  lg5-spring-acceptance-test

[Example an Acceptance Test Report with Cucumber][3]

## Principal Dependencies

```xml title="acceptance-test-module(pom.xml)" linenums="1" hl_lines="9"
<dependencies>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-starter</artifactId>
    </dependency>
</dependencies>
```

> _Use `lg5-spring-starter` to get a Spring context and have dependency injection in the test creation process._

##  Domain Service Image

!!! warning "️Warning"
    Before continuing, you will need to have the docker image of your app.
    If not, remember to add the `jib plugin` in the [container module][1] or generate the docker image in another way.
    Once you have the docker image, you can continue.

## Acceptance Test Drive Development
![Test][img_1]

!!! quote inline end ""
    ```gherkin
    Feature:
        I as a customer want to create a blank using the repository template
    
    Scenario: the blank should be CREATED when use the repository template
        Given a repository template
        When blank is created
        Then the blank will be created using the repository template
    ```

Recommended following Acceptance Test Drive Development allowed to align main goals for one or more user's needs.   

Using the traditional workflow ATDD, you need to define acceptance criteria using Gherkin syntax (Given, When, Then).

You need to add the following Dependency and Java classes to your acceptance test. So, you can try the dockerized domain service.
For more details, read more about [Hexagonal Architecture(Spanish)][2].

Dependencies:
>   _Lg5 tries to simplify dependencies but the power is the same._ 👌

```xml title="acceptance-test-module(pom.xml)" linenums="1" hl_lines="4"
<!-- Test-->
<dependency>
    <groupId>com.lg5.spring</groupId>
    <artifactId>lg5-spring-acceptance-test</artifactId>
    <scope>test</scope>
</dependency>
```
It is recommended to create the following  `boot/` directory on your `test` directory as `com.[blanksystem].[blank].service/boot`.
```markdown  hl_lines="4"
 └── test/
    ├── java/
    │  └── com.[blanksystem].[blank].service
    │     ├── boot/
    │     │  └── AcceptanceTestCase.java
    │     │  └── CucumberHooks.java
    │     │  └── TestOrderServiceApplication.java
```

To get started, you need to add the following classes:

=== "TestContainers Loader"
    > Important the image name and use the correct version. For instance: `com.blanksystem/blank-service` with version `1.0.0-alpha`.

    ```java title="TestContainersLoader.java" linenums="1" hl_lines="38"
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

    ```

=== "CucumberHooks"

    > This module does not expose any ports, so you must extend the `Lg5TestBootPortNone` class.

    ```java title="CucumberHooks.java" linenums="1" hl_lines="5 7"
    import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
    import io.cucumber.spring.CucumberContextConfiguration;
    import org.springframework.context.annotation.Import;
    
    @Import(TestContainersLoader.class)
    @CucumberContextConfiguration
    public class CucumberHooks extends Lg5TestBootPortNone {
    
    }

    ```


=== "AcceptanceTestCase"

    > 1. Define the feature file in `src/test/resources/features/*.feature`
    > 2. You can find cucumber repor in `target/atdd-reports/cucumber-reports.html`    
    > all in one click on this class, and you are ready to use the application.


    ```java title="AcceptanceTestCase.java" linenums="1" hl_lines="18 25"
    import io.cucumber.junit.platform.engine.Constants;
    import org.junit.jupiter.api.Test;
    import org.junit.platform.suite.api.ConfigurationParameter;
    import org.junit.platform.suite.api.ConfigurationParameters;
    import org.junit.platform.suite.api.IncludeEngines;
    import org.junit.platform.suite.api.SelectClasspathResource;
    import org.junit.platform.suite.api.Suite;
    
    import java.io.File;
    
    import static org.junit.jupiter.api.Assertions.assertTrue;
    
    @Suite
    @IncludeEngines("cucumber")
    @SelectClasspathResource("features")
    @ConfigurationParameters({
    @ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty, json:target/atdd-reports/cucumber.json, " +
    "html:target/atdd-reports/cucumber-reports.html"),
    @ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.blanksystem.blank.service")
    })
    class AcceptanceTestCase {
    
        @Test
        void test() {
            File feature = new File("src/test/resources/features");
            assertTrue(feature.exists());
        }
    }
    ```

=== "Features"

    > 1. Create a directory called`src/test/resources/features`
    > 2. Create the feature file in `example.feature`
    > 3. Create new step definition file

    ```gherkin title="example.feature" linenums="1" hl_lines="2 5"
    Feature:
        I as a customer want to create a blank using the repository template
    
    Scenario: the blank should be CREATED when use the repository template
        Given a repository template
        When blank is created
        Then the blank will be created using the repository template
    
    ```


    ```java title="MyStepdefs.java" linenums="1" hl_lines="2 6 10"
    public class MyStepdefs {
        @Given("a repository template")
        public void aRepositoryTemplate() {
        }
    
        @When("blank is created")
        public void blankIsCreated() {
        }
    
        @Then("the blank will be created using the repository template")
        public void theBlankWillBeCreatedUsingTheRepositoryTemplate() {
        }
    }
    ```

=== "Application Test Properties"

    With `destination.path: ./target/logs` where stored logs file from blank-service after that tests finished.     
    Replace some name by your system and domain name: `blanksystem` and `blank`.

    > `application.image.name: your-docker.images:version

    ⚠️ Please attention to highlighted lines!!!         
    ⚠️ Disabled liquibase migrations, only test(NOT PRODUCTION).

    ```yaml title="application-test.yml" linenums="1" hl_lines="5 10 15 22 43 41 53 54"
    application:
      server:
        port: 8080
      image:
        name: com.blanksystem/blank-service:1.0.0-alpha
      traces:
        console:
          enabled: false
        file:
          enabled: true
      log:
        source:
          path: /logs
        destination:
          path: ./target/logs

    blanksystem:
      blank:
        events:
          journal:
            blank:
              topic: blank.1.0.event.created
              consumer:
                group: blank-topic-consumer-acceptance-test
    spring:
      datasource:
        url: jdbc
      liquibase:
        enabled: false

    logging:
      level:
        com.blanksystem: INFO
        io.confluent.kafka: ERROR
        org.apache: ERROR

    third:
      basic:
        auth:
          username: admin
          password: pass
      jsonplaceholder:
        url: https://jsonplaceholder.typicode.com
        basic:
          auth:
            username: admin
            password: pass

    wiremock:
      config:
        folder: "wiremock/third_system/template.json"
        url: "third.jsonplaceholder.url"
        port: 7070
    ```

!!! tip "When do you like to use some TestContainer"

    if you use some TestContainer CustomConfig enabled, you would added the following properties for each one: 

        * Postgres Container
            * Add liquibase files with migrations. 
        * Kafka Container       
            * `${kafka-config.bootstrap-servers}`       
        * SchemaRegistry Container      
            * `${kafka-config.schema-registry-url}`
        * KAFKA MODELS
            * If you must have kafka models, So, CREATE AVRO DIRECTORY  For instance: `src/test/resources/avro/example.avsc`
        * Wiremock Container        
            * Specify third system url `${wiremock.config.url}`.        
            * Indicate a port binding to connect: `${wiremock.config.port}`.       
            * Directory where stored the mock req/res http `${wiremock.config.folder}`.
            * CREATE WIREMOCK DIRECTORY and a TEMPLATE base, For instance: `src/test/resources/wiremock/third_system/template.json`.

## Needs the Spring Context

Add a classic application main with Spring Boot:

> Stay alert with the `scanBasePackages` for your tests, in this case principal package the current system and extras as kafka. 

```java title="Application.java" linenums="1" hl_lines="6"
package com.blanksystem.blank.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.blanksystem", "com.lg5.spring.kafka"})
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }
}
```

### More dependencies
If you must have Kafka(avro plugin), database, etc. Please include more dependencies.
```xml linenums="1" hl_lines="5 10 15 22 41 49 50 51"
<dependencies>
    ...
    <!-- if you need to connect a database-->
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-data-jpa</artifactId>
    </dependency>
    <!-- if you need to generate models-->
    <dependency>
        <groupId>com.lg5.spring.kafka</groupId>
        <artifactId>lg5-spring-kafka-model</artifactId>
    </dependency>
    <!-- if you need to produce events-->
    <dependency>
        <groupId>com.lg5.spring.kafka</groupId>
        <artifactId>lg5-spring-kafka-producer</artifactId>
    </dependency>
    <!-- if you need to consume events-->
    <dependency>
        <groupId>com.lg5.spring.kafka</groupId>
        <artifactId>lg5-spring-kafka-consumer</artifactId>
    </dependency>
    ...
</dependencies>
...
<build>
    <plugins>
        <!-- if you need to generate models-->
        <plugin>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

     
Read more at [Lg5Spring Wiki][2].

## Project structure
```markdown linenums="1" hl_lines="7 14 15 16 19 24"
./
├── pom.xml
├── src/
│  ├── main/
│  │  ├── java/
│  │  │  └── com.blanksystem.blank.service
│  │  │     └── Application.java
│  │  └── resources/
│  │     └── application.yml
│  └── test/
│     ├── java/
│     │  └── com.blanksystem.blank.service
│     │     └── boot/
│     │        ├── AcceptanceTestCase.java
│     │        ├── CucumberHooks.java
│     │        └── TestContainersLoader.java
│     └── resources/
|        └── application-test.yml
│        ├── features/
│        │  └── blank-service.feature
│        └── wiremock/
│           └── placeholder/
│        
└── target(autogenerate)/
   └── atdd-reports/
      ├── cucumber-reports.html
      └── cucumber.json
```
!!! danger

    You do not need to create the `target/` directory, it is automatically generated by the `maven clean install` build command

## 2'DO
- [ ] **Add support for a dockerized application without port.**


[1]: container-module.md/#plugins-to-build-docker-image
[2]: https://arc.net/l/quote/hilekvaw
[3]: https://blank-service-atdd.web.app/atdd/

[img_1]: https://miro.medium.com/v2/resize:fit:1400/format:webp/1*yvkcmS-vZdx2MBifjNd5zA.png