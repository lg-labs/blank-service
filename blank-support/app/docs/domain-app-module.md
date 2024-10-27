# Application Service Domain Module

## Defined Primary Adapters

<p>
Allows the isolated domain to communicate with an outside. Orchestrate transaction, security, looking up proper aggregates and saving state changes of the domain to the database. Does not common any business logic.

Domain event listeners are special kind of Application services that are triggered by domain events. Each domain event listener can have a separate domain service to handle business logic.
</p>

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

**Part II.**
- implementing input ports

## Why use Lombok?

_Lombok is a Java library that helps to reduce boilerplate code in Java classes, such as constructors, getters, and setters. While it can be useful, some developers prefer not to use it due to concerns about code readability and maintainability._

### Why ?
**YES:** In others modules: e.g: like in the Infrastructure module will use lombook to prevent Bolirplates Java codes.