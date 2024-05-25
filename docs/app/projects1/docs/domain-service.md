## How do you implement a domain service?

**Starter** Describe step per step to implement a **_blank_ domain service**.

> ⏳ Analyse the context and begin by Acceptance Test and Domain Core...

## [Acceptance Test Module](atdd-module.md)

```xml title="pom.xml" linenums="1" hl_lines="3"
<dependency>
    <groupId>com.lg5.jvm</groupId>
    <artifactId>lg5-jvm-atdd</artifactId>
</dependency> 
```

## [Domain Core Module](domain-core-module.md)

```xml title="pom.xml" linenums="1" hl_lines="3"
<dependency>
    <groupId>lg5.common</groupId>
    <artifactId>lg5-common-domain</artifactId>
</dependency> 
```

## [Application Service Domain Module](domain-app-module.md)

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
    ...
</dependencies>
```

## [Message Module](message-module.md)

=== "message-core-module"

    ```xml title="pom.xml(core)" linenums="1" hl_lines="5 10"
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

    ```xml title="pom.xml(model)" linenums="1" hl_lines="4"
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
        <artifactId>lg5-spring-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.lg5.spring</groupId>
        <artifactId>lg5-spring-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```



