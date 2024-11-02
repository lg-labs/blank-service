# Application Service Domain Module

## Defined Primary Adapters

Allows the isolated domain to communicate with an outside. Orchestrate transaction, security, looking up proper aggregates and saving state changes of the domain to the database. It does not common any business logic.

Domain event listeners are special kind of Application services that are triggered by domain events. Each domain event listener can have a separate domain service to handle business logic.


!!! quote ""
    !!! quote inline ""
    
        **Part I.**

        - DTOs
          - Mappers
          - Exceptions
          - Ports
              - Input
                  - Listeners
                  - Domain Service
              - Output
                  - Publishers
                  - Repositories
    
    
    !!! quote inline ""
        **Part II**
    
        - Implementing input ports


## Why use Lombok?

_Lombok is a Java library that helps to reduce boilerplate code in Java classes, such as constructors, getters, and setters. While it can be useful, some developers prefer not to use it due to concerns about code readability and maintainability._

### Why ?
**YES:** In others modules: e.g.: like in the Infrastructure module will use lombok to prevent Boilerplate Java codes.

## Dependencies

=== "blank-application-service(pom.xml)"

    ```xml title="blank-application-service(pom.xml)" linenums="1" hl_lines="5 10 12"
    <dependencies>
        <!-- µ-service dependencies -->
        <dependency>
            <groupId>com.blanksystem</groupId>
            <artifactId>blank-domain-core</artifactId>
        </dependency>
        <!-- lg5 dependencies -->
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
    </dependencies>
    ```
=== "Principal"

    ```xml title="pom.xml" linenums="1" hl_lines="4"
    <!-- lg5 dependencies -->
    <dependency>
        <groupId>lg5.common</groupId>
        <artifactId>lg5-common-application-service</artifactId>
    </dependency>
    ```
=== "µ-service dependencies"

    ```xml title="pom.xml" linenums="1" hl_lines="4"
    <!-- µ-service dependencies -->
    <dependency>
        <groupId>com.blanksystem</groupId>
        <artifactId>blank-domain-core</artifactId>
    </dependency>
    ```
=== "Extras"

    ```xml title="pom.xml" linenums="1" hl_lines="5 9"
    <dependencies>
        <!-- if you need SAGA Pattern/Outbox Pattern, else remove dependencies -->
        <dependency>
            <groupId>com.lg5.spring.outbox</groupId>
            <artifactId>lg5-spring-outbox</artifactId>
        </dependency>
        <dependency>
            <groupId>com.lg5.jvm</groupId>
            <artifactId>lg5-jvm-saga</artifactId>
        </dependency>
    </dependencies>
    ```
### Unit Test to Use Case

If you need to create tests (only unit test for this module), you can add the following dependency:


```xml title="pom.xml" linenums="1" hl_lines="4 10"
<!--unit test -->
<dependency>
    <groupId>com.lg5.jvm</groupId>
    <artifactId>lg5-jvm-unit-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Structure Project


```markdown linenums="1" hl_lines="3 6 8 15 17 19 25 33 43"
com.blanksystem.blank.service
└──domain/
   ├── BlankApplicationServiceImpl.java
   ├── BlankCreateCommandHandler.java
   ├── BlankMessageListenerImpl.java
   ├── config/
   │  └── BlankServiceConfigData.java
   ├── dto/
   │  ├── create/
   │  │  ├── CreateBlankCommand.java
   │  │  └── CreateBlankResponse.java
   │  ├── message/
   │  │  └── BlankModel.java
   │  └── track/
   ├── exception/
   │  └── BlankApplicationServiceException.java
   ├── mapper/
   │  └── BlankDataMapper.java
   ├── outbox/
   │  ├── model/
   │  │  └── ...
   │  └── scheduler/
   │     └── ...
   ├── ports/
   │  ├── input/
   │  │  ├── message/
   │  │  │  └── listener/
   │  │  │     ├── blank/
   │  │  │     │  └── BlankMessageListener.java
   │  │  │     └── customer/
   │  │  └── service/
   │  │     └── BlankApplicationService.java
   │  └── output/
   │     ├── message/
   │     │  └── publisher/
   │     │     ├── BlankMessagePublisher.java
   │     │     ├── payment/
   │     │     └── restaurantapproval/
   │     └── repository/
   │        ├── BlankRepository.java
   │        ├── OtherRepository.java
   │        └── ReportRepository.java
   └── saga/
       └── ...

```