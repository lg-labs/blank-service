# Container Module
Part I.

- Main Application
- Create Beans(e.g: Domain Service)
- Configurations
- Properties

## Dependencies
```xml title="pom.xml" linenums="1" hl_lines="4 9"
<dependencies>
    <dependency>
        <groupId>com.blanksystem</groupId>
        <artifactId>blank-application-service</artifactId>
    </dependency>
    <!--lg5 dependencies-->
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-api-rest</artifactId>
    </dependency>
</dependencies>
```

## Plugins to build docker image 

It has two plugins prepared for build image. Only, you need to add the plugin.

!!! tip 

    The image name is `com.blanksystem/blank-service:1.0.0-alpha`   
    Given from maven **parent module** as `group/artefact:version`  
    Also -> `com.[system]/[domain]-service:[current_version]`

=== "Using Jib Plugin"

    ```xml title="pom.xml" linenums="1" hl_lines="2 9"
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

=== "Using spring-boot-maven-plugin"

    ```xml title="pom.xml" linenums="1" hl_lines="5"
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    ```

## Integration Test with Lg5Container

You need to add the following Java classes to the test directory.

=== "Dependencies"

    ```xml title="pom.xml" linenums="1" hl_lines="3 8"
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    ```

=== "TestContainers Loader"

    ```java title="TestContainersLoader.java" linenums="1" hl_lines="6"
    import com.lg5.spring.testcontainer.DataBaseContainerCustomConfig;
    import com.lg5.spring.testcontainer.KafkaContainerCustomConfig;
    import com.lg5.spring.testcontainer.WiremockContainerCustomConfig;
    import org.springframework.context.annotation.Import;
    
    @Import({
        DataBaseContainerCustomConfig.class,
        KafkaContainerCustomConfig.class,
        WiremockContainerCustomConfig.class
    })
    public final class TestContainersLoader {
    }
    ```

=== "Bootstrap"
    _For create a Bootstrap class has two superclasses:     
    — If the APP expose any port as `:8080`, so you must extend of `Lg5TestBoot` class            
    — Else, extend of `Lg5TestBootPortNone` class_

      ```java title="Bootstrap.java" linenums="1" hl_lines="5"
      import com.lg5.spring.testcontainer.Lg5TestBoot;
      import org.springframework.context.annotation.Import;
    
      @Import(TestContainersLoader.class)
      public abstract class Bootstrap extends Lg5TestBoot {
      }
      ```

=== "TestApplication(Optional)"

    ```java title="TestApplication.java" linenums="1" hl_lines="10 11"
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
The TestContainer CustomConfig already to use:  

* DataBaseContainerCustomConfig
* KafkaContainerCustomConfig
* WiremockContainerCustomConfig

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
For instance at Wiremock Container:
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
    │     ├── api/
    │     │  └── ...
    │     ├── data/
    │     │  └── ...
    │     ├── external/
    │     │  └── ...
    │     ├── repository/
    │     │  └── ...
    │     ├── support/
    │     │  └── ...
    │     ├── [*]TestApplication.java
    │     ├── [*]Bootstrap.java
    │     └── [*]TestContainersLoader.java
    └── resources/
       ├── config/
       │  └── application-test.yaml
       └── wiremock/
          └── third_system/
             └── template.json
``` 

> **[*]** There are Java classes to the test directory.

## 2'DO

#### Add more TestContainer custom
  * AWS Services(S3)
  * sftp
  * more third services.