# Domain Core Module

## Business Logic

Business logic that cannot fit in the aggregate. Used when multiple aggregates required in business logic Can interact with other domain services.

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

### [🕹️ Plugins to auto-generate builder without losing control](https://plugins.jetbrains.com/plugin/7354-innerbuilder)

## Dependencies

```xml title="pom.xml" linenums="1" hl_lines="4 10"
<dependency>
    <groupId>lg5.common</groupId>
    <artifactId>lg5-common-domain</artifactId>
</dependency>
```

## Unit Test to Use Case



