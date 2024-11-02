# API Module

Implement input port from a primary adapter. For instance, Spring Rest Controller.

[Example: a spec with OpenAPI][2]


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
### Global Error Handler
To Api REST, you need to add the following class with a global configuration to http errors.
```java linenums="1"
import com.blanksystem.blank.service.domain.exception.BlankDomainException;
import com.blanksystem.blank.service.domain.exception.BlankNotFoundException;
import com.lg5.spring.api.rest.ErrorDTO;
import com.lg5.spring.api.rest.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <h1>Caught the Blank Domain Layer errors</h1>
 */
@Slf4j
@ControllerAdvice
public class BlankGlobalExceptionHandler extends GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = {BlankDomainException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleException(BlankDomainException orderDomainException) {
        log.error(orderDomainException.getMessage(), orderDomainException);
        return new ErrorDTO(HttpStatus.BAD_REQUEST.getReasonPhrase(), orderDomainException.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = {BlankNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleException(BlankNotFoundException orderNotFoundException) {
        log.error(orderNotFoundException.getMessage(), orderNotFoundException);
        return new ErrorDTO(HttpStatus.NOT_FOUND.getReasonPhrase(), orderNotFoundException.getMessage());
    }
}
```
# Add OpenAPI file

You must define an api before that written or autogenerate the controllers:     
Create `src/main/resources/spec/openapi.yaml` as recommendation.


# Service Healthy
By default, has a health endpoint as GET method to `localhost:PORT/health`.     
And, expected the response status with `200 OK`
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
Remember, you must add the Open Api definition into `resourses/spec/openapi.yaml`

_Read more about [openapi guidelines][1]_ 


## 2'DO
#### Extend support to:
* GraphQL
* gRCP
* Command Line Interactive or others.

[1]: https://lufgarciaqu.medium.com
[2]: https://blank-service-atdd.web.app/openapi/