# Domain Core Module

## Business Logic

Business logic that cannot fit in the aggregate. Used when multiple aggregates required in business logic can interact with other domain services.

!!! quote ""
    !!! quote inline ""
        Use or need of common domain module.

        - BaseEntity
        - AggregateRoot
        - BaseId(e.g: V.O)
    
    !!! quote inline ""
    
        **Part I.**
    
        - Aggregate Root
        - Entities
        - Value Object
    
    
    !!! quote inline ""
        **Part II**
    
        - Exception classes
        - Domain Events
        - Domain Service

## Why not use Lombok

_Lombok is a Java library that helps to reduce boilerplate code in Java classes, such as constructors, getters, and setters. While it can be useful, some developers prefer not to use it due to concerns about code readability and maintainability._

### Why no?
**NOT:** For the core logic, is not recommended. It is a bad idea use any framework that autogenare code, becasue, you need to see code directly, maybe add any validation.

### [🕹️ Plugins to auto-generate builder without losing control][1]

## Dependencies

```xml title="pom.xml" linenums="1" hl_lines="3"
<dependency>
    <groupId>lg5.common</groupId>
    <artifactId>lg5-common-domain</artifactId>
</dependency>
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

```markdown linenums="1" hl_lines="3 5 7 10 13"
com.blanksystem.blank.service
└── domain/
    ├── BlankDomainService.java
    ├── BlankDomainServiceImpl.java
    ├── entity/
    │  └── Blank.java
    ├── event/
    │  ├── BlankCreatedEvent.java
    │  └── BlankEvent.java
    ├── exception/
    │  ├── BlankDomainException.java
    │  └── BlankNotFoundException.java
    └── valueobject/
       └── BlankId.java
```


[1]: https://plugins.jetbrains.com/plugin/7354-innerbuilder