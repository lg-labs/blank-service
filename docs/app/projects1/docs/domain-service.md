## How do you implement a domain service?

**Starter** Describe step per step to implement a **_blank_ domain service**.

> ⏳ Analyse the context and begin by Acceptance Test and Domain Core...

## Acceptance Test Module

Use or need of acceptance test module


## Domain Core Module

Use or need of common domain module

- BaseEntity
- AggregateRoot
- BaseId(e.g: V.O)

**Part I.**

- Aggregate Root
- Entities
- Value Object

**Part II**

- Exception classes
- Domain Events
- Domain Service

## Application Service Domain Module

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

## Data Access Module

Part I.

- Secondary Adapter to DB
- Mapper
- Explicit Repositories to DB

## Messaging Module

Part I.

- Models from Event Specification(e.g: Avro Models)
- Mappers

Part II.

- Implementing output ports(Publishers/Producers)
    - Secondary Adapter
- Implementing input ports(Listener/Consumers)
    - Primary Adapter

## Container

Part I.

- Main Application
- Create Beans(e.g: Domain Service)
- Configurations
- Properties



