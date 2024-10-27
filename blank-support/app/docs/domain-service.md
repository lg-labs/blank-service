## Define Principal Module or Parent Module

> Using Lg5 Spring Framework `1.0.0-alpha`, JDK 21  
> [More details][1]

## Dependencies diagram
![][ima_1]

## Parent Module
```xml title="pom.xml" linenums="1" hl_lines="4 10"
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-parent</artifactId>
        <version>1.0.0-alpha.[check lts version]</version>
        <relativePath/>
    </parent>
    
    <groupId>com.blanksystem</groupId>
    <artifactId>blank-service</artifactId>
    <version>1.0.0-alpha</version>
    <packaging>pom</packaging>
    <modules>
        ...
    </modules>
    
    <dependencyManagement>
        <dependencies>
            ...
        </dependencies>
    </dependencyManagement>
</project>
```
_Note: Please check the [latest version][2]_

## How do you implement a domain service?

**Starter** Describe step per step to implement a **_blank_ domain service**.

> ⏳ Analyse the context and begin by Acceptance Test and Domain Core...

## [Acceptance Test Module](atdd-module.md)

```xml title="pom.xml" linenums="1" hl_lines="3"
<dependency>
    <groupId>com.lg5.spring</groupId>
    <artifactId>lg5-spring-integration-test</artifactId>
</dependency> 
```

## [Domain Module](domain-core-module.md)

=== "Domain Core Module"

    [Domain Core Module](domain-core-module.md)
    ```xml title="pom.xml" linenums="1" hl_lines="3"
    <dependency>
        <groupId>lg5.common</groupId>
        <artifactId>lg5-common-domain</artifactId>
    </dependency> 
    ```

=== "Application Service Domain Module"

    [Application Service Domain Module](domain-app-module.md)
    ```xml title="pom.xml" linenums="1" hl_lines="4 9 13"
    <dependencies>
        <dependency>
            <groupId>lg5.common</groupId>
            <artifactId>lg5-common-application-service</artifactId>
        </dependency>
        <!-- if you need SAGA Pattern/Outbox Pattern, else remove dependencies -->
        <dependency>
            <groupId>com.lg5.spring.outbox</groupId>
            <artifactId>lg5-spring-outbox</artifactId>
        </dependency>
        <dependency>
            <groupId>com.lg5.jvm</groupId>
            <artifactId>lg5-jvm-saga</artifactId>
        </dependency>
        ...
    </dependencies>
    ```

## [Data Access Module](data-module.md)

```xml title="pom.xml" linenums="1" hl_lines="4 9"
<dependencies>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-data-jpa</artifactId>
    </dependency>
    <!-- if you need SAGA Pattern/Outbox Pattern, else remove dependencies -->
    <dependency>
        <groupId>com.lg5.spring.outbox</groupId>
        <artifactId>lg5-spring-outbox</artifactId>
    </dependency>
</dependencies>
```

## [Message Module](message-module.md)

=== "message-core-module"

    ```xml title="pom.xml" linenums="1" hl_lines="5 10"
        <dependencies>
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
    ```

=== "message-model-module"

    ```xml title="pom.xml" linenums="1" hl_lines="4"
        <dependencies>
            <dependency>
                <groupId>com.lg5.spring.kafka</groupId>
                <artifactId>lg5-spring-kafka-model</artifactId>
            </dependency>
            ...
        </dependencies>
    ```

## [External Module](external-module.md)
```xml title="pom.xml" linenums="1" hl_lines="4"
    <dependencies>
        <dependency>
            <groupId>com.lg5.spring</groupId>
            <artifactId>lg5-spring-client</artifactId>
        </dependency>
        ...
    </dependencies>
```

## [API Module](api-module.md)
```xml title="pom.xml" linenums="1" hl_lines="4"
<dependencies>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-api-rest</artifactId>
    </dependency>
    ...
</dependencies>
```

## [Container Module](container-module.md)
```xml title="pom.xml" linenums="1" hl_lines="4 8 13 18"
<dependencies>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-logger</artifactId>
    </dependency>
    <!-- tests -->
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-integration-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```


## Project structure
```markdown
└── blank-service
   ├── blank-acceptance-test
   │  └── pom.xml
   ├── blank-api
   │  └── pom.xml
   ├── blank-container
   │  └── pom.xml
   ├── blank-data-access
   │  └── pom.xml
   ├── blank-domain
   │  └── pom.xml
   ├── blank-external
   │  └── pom.xml
   ├── blank-message
   │  └── pom.xml
   └── pom.xml
``` 


[1]: https://lg-labs-pentagon.github.io/lg5-spring/
[2]: https://github.com/lg-labs-pentagon/lg5-spring/packages/2125499

[ima_1]: img/dependency-graph.png