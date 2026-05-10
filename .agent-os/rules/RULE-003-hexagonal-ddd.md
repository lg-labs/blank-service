---
id: RULE-003
slug: hexagonal-ddd
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: architecture
tags: [hexagonal, ddd, domain-purity, ports-and-adapters]
description: Hexagonal architecture + DDD. Domain logic lives in `<svc>-domain-core` and depends on nothing Spring. Spring annotations belong in adapters / application-service / container.
---

# RULE-003 — Hexagonal architecture + DDD

## Statement

Every microservice follows **Hexagonal (ports & adapters) + DDD**:

- `<svc>-domain-core`: aggregates, value objects, domain events, domain
  services, **input/output ports** (interfaces). Pure Java. **No Spring**, no
  JPA, no Jackson, no Lombok beyond `@Builder`/`@Getter` where useful.
- `<svc>-application-service`: orchestrates use cases by depending on the
  domain ports. Contains command handlers, the saga step implementations, the
  outbox payload mappers. Spring `@Component`/`@Service`/`@Transactional`
  allowed here.
- `<svc>-data-access`, `<svc>-message`, `<svc>-api`, `<svc>-external`:
  adapter modules that **implement** the output ports declared in
  `domain-core` and translate the input ports into HTTP/Kafka/JPA.
- `<svc>-container`: the only module with `@SpringBootApplication`,
  `application.yaml`, and the runtime composition root.

## Rationale

The domain is the single piece of code with the longest expected lifetime and
the highest cost of breakage. Keeping it framework-free means it can be
unit-tested in milliseconds without Spring context, ported to a new framework
generation, and reasoned about purely in business terms. Putting Spring
annotations in `domain-core` couples the domain model to the framework's
lifecycle and breaks the dependency rule: outer rings depend inward, never the
reverse.

## Example — correct

```
order-domain-core/
  src/main/java/com/example/order/domain/
    Order.java                       // pure Java aggregate root
    OrderId.java                     // value object
    event/OrderCreatedEvent.java     // pure Java domain event
    ports/input/OrderApplicationService.java   // port (interface only)
    ports/output/OrderRepository.java          // port (interface only)
    exception/OrderDomainException.java
```

```java
// order-domain-core/src/main/java/.../Order.java
public class Order extends AggregateRoot<OrderId> {
    public OrderCreatedEvent place(final Money total) {
        // pure business logic, no Spring imports anywhere in this file
    }
}
```

```java
// order-application-service/src/main/java/.../OrderCommandHandler.java
@Service                                       // Spring lives HERE
@RequiredArgsConstructor
public class OrderCommandHandler implements OrderApplicationService {
    private final OrderRepository repository;  // depends on the PORT
    @Transactional
    public OrderResponse place(final PlaceOrderCommand cmd) { ... }
}
```

## Anti-pattern

```java
// WRONG: Spring annotation inside domain-core
package com.example.order.domain;

@Entity                                        // ❌ JPA in domain
@Table(name = "orders")                        // ❌ infrastructure leak
public class Order {
    @Id @GeneratedValue
    private UUID id;

    @Autowired                                 // ❌ DI in a domain object
    private transient PaymentService paymentService;
}
```

This couples the aggregate to JPA's lifecycle, makes unit testing require an
EntityManager, and leaks the persistence model into the business model.

## References

- Skill: `lg5-new-service` (module skeleton step-by-step).
- Skill: `lg5-spring-overview` (architecture diagram).
- Related rules: RULE-004 (module shape), RULE-005 (no custom annotations),
  RULE-016 (DDD building blocks come from external library).
