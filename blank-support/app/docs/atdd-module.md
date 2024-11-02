# 🧪Acceptance Test(ATDD) Module

!!! Success "Good news for you 🎉"
    Finally, Lg5 supports Cucumber integration with Spring Boot 3, Testcontainers, and reuses many configurations for testing. This way, you can create acceptance tests for your domain services quickly.
    >  lg5-spring-acceptance-test

[Example a Acceptance Test Report with Cucumber][3]

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

```xml title="acceptance-test-module(pom.xml)" linenums="1" hl_lines="3"
<dependency>
    <groupId>com.lg5.spring</groupId>
    <artifactId>lg5-spring-integration-test</artifactId>
    <scope>test</scope>
</dependency>
```
It is recommended to create the following  `boot/` directory on your test directory.
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

    ```java title="TestContainersLoader.java" linenums="1" hl_lines="32"
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

    ```

=== "CucumberHooks"

    > This module does not expose any ports, so you must extend the `Lg5TestBootPortNone` class.

    ```java title="CucumberHooks.java" linenums="1" hl_lines="5 7"
    import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
    import io.cucumber.spring.CucumberContextConfiguration;
    import org.springframework.context.annotation.Import;
    
    @CucumberContextConfiguration
    @Import(TestContainersLoader.class)
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

!!! tip "When do you like to use some TestContainer"

    if you use some TestContainer CustomConfig enabled, you would added the following properties for each one: 

        * Postgres Container            
        * Kafka Container       
            * `${kafka-config.bootstrap-servers}`       
        * SchemaRegistry Container      
            * `${kafka-config.schema-registry-url}`     
        * Wiremock Container        
            * Specify third system url `${wiremock.config.url}`.        
            * Indicate a port binding to connect: `${wiremock.config.port}`.       
            * Directory where stored the mock req/res http `${wiremock.config.folder}`.
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
│        ├── features/
│        │  └── blank-service.feature
│        └── wiremock/
│           └── placeholder/
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