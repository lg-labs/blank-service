# Container Module

!!! abstract inline end ""
    - Main Application
    - Create Beans(e.g.: Domain Service)
    - Configurations
    - Properties.

This module has control over the service and injects different packages before building the app, Good Look!
It is straightforward and has a single responsibility (S of SOLID) 😎.
One thing more, You need to create a dependency injection for your domain service;
this modulo should be to isolate any framework with domain module(D of SOLID).



## Principal Dependencies
```xml title="container-module(pom.xml)" linenums="1" hl_lines="4"
<dependencies>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-starter</artifactId>
    </dependency>
</dependencies>
``` 
## Your modules as dependency
For this case, you have an app with the follows modules, for instance:  

!!! info inline "Modules"
    - blank-application-service
    - blank-api
    - blank-data-access
    - blank-message-core
    - blank-external

```xml title="container-module(pom.xml)" linenums="1" hl_lines="4 9"
<dependencies>
    <dependency>
        <groupId>com.blanksystem</groupId>
        <artifactId>blank-application-service</artifactId>
    </dependency>
    <!--add all modules as api, data-access..etc..-->
</dependencies>
```


## Plugins to build docker image 

Using jib plugins prepared for build image. Only, you need to add the plugin.

!!! tip 

    The image name is `com.blanksystem/blank-service:1.0.0-alpha`   
    Given from maven **parent module** as `groupId/artifactId:version`  
    Also -> `com.[system]/[domain]-service:[current_version]`
```xml title="container-module(pom.xml)" linenums="1" hl_lines="2 9"
    <properties>
        <docker.from.image.platform.architecture>arm64</docker.from.image.platform.architecture>
        <docker.from.image.platform.os>linux</docker.from.image.platform.os>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```


As a final result, you'll have a docker image `com.blanksystem/blank-service:1.0.0-alpha`. By default, for Arch AMD. 
Also, you can build docker image to **Arch AMD** using make build_to_amd, or **Arch Linux ARM** using `make build_to_arm`.

_----Experimental to AMD----_

## Integration Test with Lg5Container

You need to add the following Dependency and Java classes to your integration test (IT). _Recommendation(optional)_: Remember to create IT in this module for infrastructure components such as input and output ports.
For more details, read more about [Hexagonal Architecture(Spanish)][1].

Dependencies:   
>   _Lg5 tries to simplify dependencies but the power is the same._ 👌

```xml title="container-module(pom.xml)" linenums="1" hl_lines="3 8"
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
    │  └── com.[blanksystem].[blank].service.container
    │     ├── boot/
    │     │  └── Bootstrap.java
    │     │  └── TestContainersLoader.java
    │     │  └── TestOrderServiceApplication.java
```
=== "TestContainers Loader"

    ```java title="TestContainersLoader.java" linenums="1" hl_lines="6"
    import com.lg5.spring.testcontainer.config.KafkaContainerCustomConfig;
    import com.lg5.spring.testcontainer.config.PostgresContainerCustomConfig;
    import com.lg5.spring.testcontainer.config.WiremockContainerCustomConfig;
    import org.springframework.context.annotation.Import;
    
    @Import({
        PostgresContainerCustomConfig.class,
        KafkaContainerCustomConfig.class,
        WiremockContainerCustomConfig.class,
    })
    public final class TestContainersLoader {
    }
    ```

=== "Bootstrap"
    >   _For create a Bootstrap class has two superclasses:     
    >   — If the APP expose any port as `:8080`, so you must extend of `Lg5TestBoot` class            
    >   — Else, extend of `Lg5TestBootPortNone` class_

      ```java title="Bootstrap.java" linenums="1" hl_lines="5"
      import com.lg5.spring.testcontainer.boot.Lg5TestBoot;
      import org.springframework.context.annotation.Import;
    
      @Import(TestContainersLoader.class)
      public abstract class Bootstrap extends Lg5TestBoot {
      }
      ```

=== "TestApplication(Optional)"
    > Testcontainers during the development time or run the app locally, from Spring Boot `3.1.0` this is possible.
    > Avoid Docker configuration extras like Dockerfiles and others.    
    > all in one click on this class, and you are ready to use the application. 

    🚨 DO NOT USE FOR PRODUCTION OR LIVING ENVIRONMENTS.

    ```java title="TestApplication.java" linenums="1" hl_lines="6 10 11"
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.test.context.TestConfiguration;
    import org.springframework.context.annotation.Import;
    
    @TestConfiguration(proxyBeanMethods = false)
    @Import(TestContainersLoader.class)
    class TestApplication {

        public static void main(String[] args) {
            SpringApplication.from(Application::main)
                    .with(TestApplication.class)
                    .run(args);
        }

    }
    ```
Some TestContainer CustomConfig already to use:  

* PostgresContainerCustomConfig
* KafkaContainerCustomConfig
* WiremockContainerCustomConfig
* WireMockGuiContainerCustomConfig

Read more at [Lg5Spring Wiki][2]. 

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
For instance, in Wiremock Container:
```yaml title="application.yaml" hl_lines="1 2 3 12 18"
third:
  jsonplaceholder:
    url: https://jsonplaceholder.typicode.com
    basic:
      auth:
        username: admin
        password: pass
feign:
  client:
    config:
      jsonplaceholder:
        url: ${third.jsonplaceholder.url}
      default:
        loggerLevel: FULL
wiremock:
  config:
    folder: "wiremock/third_system/template.json"
    url: "third.jsonplaceholder.url"
    port: 7070
```
## Project structure
```markdown linenums="1" hl_lines="25 26 27 31 32 33"
 ├── main/
 │  ├── java/
 │  │  └── com.blanksystem.blank.service.container
 │  │     ├── Application.java
 │  │     └── BeanConfiguration.java
 │  └── resources/
 │     ├── config/
 │     │  ├── application-local.yaml
 │     │  ├── application.yaml
 │     │  └── bootstrap.yml
 │     └── logback-spring.xml
 └── test/
    ├── java/
    │  └── com.blanksystem.blank.service.container
    │     ├── boot/
    │     │  └── [*] Bootstrap.java
    │     │  └── [*] TestContainersLoader.java
    │     │  └── [*] TestOrderServiceApplication.java
    │     ├── api/
    │     │  └── ...
    │     ├── data/
    │     │  └── ...
    │     ├── external/
    │     │  └── ...
    │     ├── repository/
    │     │  └── ...
    │     └── support/
    │        └── ...
    └── resources/
       ├── config/
       │  └── application-test.yaml
       └── wiremock/
          └── third_system/
             └── template.json
``` 

> **[*]** There are Java classes to the test directory.

## Logger & ELK
This project is prepared to send log files and process visualization with filebeat.
You can specify the directory for stored the *.log files. Now, genera two file logs.

> Simple log
>* `[log.path]/[application_name]-simple.log`
>
> Complex log
>* `    [log.path]/[application_name]-complex.log`
>
- Specify the directory with `log.path` property.

**_Simple_**: `Simple details about application logs.`
**_Complex_**:  `More details about application logs.` 


## 2'DO

- [ ] **Add more Testcontainers custom:**
    * AWS Services(S3, SQS, SNS...)
    * sftp
    * third services

[1]: https://arc.net/l/quote/jkzommbu
[2]: https://arc.net/l/quote/ryfweuos
