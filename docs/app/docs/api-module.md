# API Module

Implement input port from a primary adapter. For instance, Spring Rest Controller.


## Dependencies
```xml title="pom.xml" linenums="1" hl_lines="9"
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

## Project structure
```markdown linenums="1" hl_lines="11"
...
└── com.blanksystem.blank.service.api
   ├── exception/
   │  └── handler/
   │     └── BlankGlobalExceptionHandler.java
   └── rest/
      └── BlankController.java
...
└── resources/
    └── spec/
       └── openapi.yaml
``` 

## 2'DO
#### Extend support to:
* GraphQL
* gRCP
* Command Line Interactive or others.