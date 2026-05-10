---
name: lg5-saga
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: How to implement a SagaStep<T> in lg5-spring — orchestration, idempotency, optimistic locking, transactional boundaries. Load this skill when the user asks about sagas, saga steps, choreography, distributed transactions, compensating actions, or wants to add/modify a saga in a service.
---

# lg5-spring — Saga Step Pattern

> Framework abstraction: `com.lg5.spring.saga.SagaStep<T>` (Kotlin interface in `lg5-jvm-saga`).
> Reference impl: `/tmp/lg5-study/food-ordering-system/order-service/order-domain/order-application-service/src/main/java/com/labs/lg/food/ordering/system/order/service/domain/OrderPaymentSaga.java`.

## The contract

```kotlin
// /tmp/lg5-study/lg5-spring/lg5-jvm-saga/src/main/kotlin/com/lg5/spring/saga/SagaStep.kt
package com.lg5.spring.saga

interface SagaStep<T> {
    fun process(data: T)
    fun rollback(data: T)
}
```

Two methods. No magic. The framework provides nothing else — orchestration, persistence, and event flow are your responsibility.

## Mental model

A Saga is a **sequence of local transactions** orchestrated through Kafka events. Each `SagaStep` reacts to one inbound event and:

1. **Loads outbox state** for `(sagaId, SagaStatus.STARTED)`. If missing → already processed → return (idempotency).
2. **Applies the domain event** to its aggregate (mutating state).
3. **Saves the next outbox message** (`OutboxStatus.STARTED`) so the next step can be triggered by the scheduler.
4. **Updates the current outbox** to reflect the new SagaStatus.

`rollback` does the symmetric compensation.

## Canonical implementation

```java
// food-ordering-system/order-service/.../OrderPaymentSaga.java
@Slf4j
@Component
public class OrderPaymentSaga implements SagaStep<PaymentResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    // ctor injection (final fields, no @Autowired)

    @Override
    @Transactional
    public void process(final PaymentResponse paymentResponse) {
        final Optional<OrderPaymentOutboxMessage> outboxOpt =
            paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
                UUID.fromString(paymentResponse.getSagaId()),
                SagaStatus.STARTED);

        if (outboxOpt.isEmpty()) {
            log.info("Outbox message already processed for sagaId: {}", paymentResponse.getSagaId());
            return;                                            // ← idempotency guard
        }
        final OrderPaymentOutboxMessage outbox = outboxOpt.get();

        final OrderPaidEvent domainEvent = completePaymentForOrder(paymentResponse);

        final SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(
            domainEvent.getOrder().getOrderStatus());

        paymentOutboxHelper.save(getUpdatedPaymentOutboxMessage(
            outbox, domainEvent.getOrder().getOrderStatus(), sagaStatus));

        approvalOutboxHelper.saveApprovalOutboxMessage(
            orderDataMapper.orderPaidEventToOrderApprovalEventPayload(domainEvent),
            domainEvent.getOrder().getOrderStatus(),
            sagaStatus,
            OutboxStatus.STARTED,
            UUID.fromString(paymentResponse.getSagaId()));
    }

    @Override
    @Transactional
    public void rollback(final PaymentResponse paymentResponse) {
        final Optional<OrderPaymentOutboxMessage> outboxOpt =
            paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
                UUID.fromString(paymentResponse.getSagaId()),
                getCurrentSagaStatus(paymentResponse.getPaymentStatus()));

        if (outboxOpt.isEmpty()) {
            log.info("Payment rollback outbox already processed: {}", paymentResponse.getSagaId());
            return;
        }
        // … compensation logic, update outbox to COMPENSATED
    }
}
```

## Mandatory rules

1. **`@Component`** at class level, **`@Transactional`** on `process` and `rollback`.
2. **Constructor injection** with `final` fields. No `@Autowired` on fields.
3. **Idempotency**: always start by querying the outbox by `(sagaId, expected SagaStatus)`. Return early if absent.
4. **Optimistic locking**: outbox JPA entities **must** have `@Version` (see `lg5-outbox` skill).
5. **No rethrow** from listeners that invoke this step on `OptimisticLockingFailureException` — let it become a NO-OP (see `lg5-kafka-avro` skill).
6. **Same transaction = aggregate update + outbox save**. Never split them.
7. **Forward chaining via outbox**: never call the next saga step directly. Always insert a new outbox row in `STARTED` and let the relay scheduler emit the Kafka event.

## SagaStatus enum (per service)

Defined per-service (NOT in the framework). Typical values:

```java
public enum SagaStatus {
    STARTED,          // outbox row inserted, Kafka emit pending
    PROCESSING,       // intermediate (e.g. payment ok, awaiting approval)
    SUCCEEDED,        // happy-path terminal
    COMPENSATING,     // rollback in flight
    COMPENSATED,      // rollback terminal
    FAILED            // unrecoverable
}
```

## Command-Handler / Helper split (transactional boundary)

food-ordering-system uses a consistent two-class pattern for use cases that mutate state and emit outbox messages. **The transactional boundary lives on the helper, not on the listener/handler.**

### Pattern A — Handler wraps Helper (order-service)

Both layers are `@Transactional` (default `REQUIRED`, so they share one tx):

```java
// OrderCreateCommandHandler.java
@Component @RequiredArgsConstructor
public class OrderCreateCommandHandler {
    private final OrderCreateHelper      orderCreateHelper;
    private final PaymentOutboxHelper    paymentOutboxHelper;
    private final OrderSagaHelper        orderSagaHelper;
    private final OrderDataMapper        orderDataMapper;

    @Transactional
    public CreateOrderResponse createOrder(final CreateOrderCommand cmd) {
        final OrderCreatedEvent ev = orderCreateHelper.persistOrder(cmd);   // domain + JPA save
        paymentOutboxHelper.savePaymentOutboxMessage(                       // synchronous outbox write
            orderDataMapper.orderCreatedEventToOrderPaymentEventPayload(ev),
            ev.getOrder().getOrderStatus(),
            orderSagaHelper.orderStatusToSagaStatus(ev.getOrder().getOrderStatus()),
            OutboxStatus.STARTED,
            UUID.randomUUID());
        return orderDataMapper.orderToCreateOrderResponse(ev.getOrder(), "Order created");
    }
}
```

```java
// OrderCreateHelper.java — owns the aggregate persistence step
@Component @RequiredArgsConstructor
public class OrderCreateHelper {
    @Transactional
    public OrderCreatedEvent persistOrder(final CreateOrderCommand cmd) {
        checkCustomer(cmd.customerId());
        final Restaurant restaurant = checkRestaurant(cmd);
        final Order order = orderDataMapper.createOrderCommandToOrder(cmd);
        final OrderCreatedEvent ev = orderDomainService.validateAndInitiateOrder(order, restaurant);
        saveOrder(order);
        return ev;
    }
}
```

### Pattern B — Thin listener delegates to Transactional Helper (payment-service)

The Kafka listener / message-listener impl is **NOT** annotated `@Transactional`; only the helper is. This keeps the Tx boundary tight around DB work and lets the listener stay free to swallow `OptimisticLockingFailureException` outside the transaction.

```java
// PaymentRequestMessageListenerImpl.java — no @Transactional
@Component @RequiredArgsConstructor
public class PaymentRequestMessageListenerImpl implements PaymentRequestMessageListener {
    private final PaymentRequestHelper helper;

    @Override public void completePayment(final PaymentRequest req) { helper.persistPayment(req); }
    @Override public void cancelPayment(final PaymentRequest req)  { helper.persistCancelPayment(req); }
}
```

```java
// PaymentRequestHelper.java — owns the transaction
@Component @RequiredArgsConstructor
public class PaymentRequestHelper {

    @Transactional
    public void persistPayment(final PaymentRequest req) {
        if (publishIfOutboxMessageProcessedForPayment(req, PaymentStatus.COMPLETED)) return;  // idempotency
        final PaymentEvent ev = paymentDomainService.validateAndInitiatePayment(...);
        persistDbObjects(payment, creditEntry, creditHistories, failureMessages);
        orderOutboxHelper.saveOrderOutboxMessage(
            paymentDataMapper.paymentEventToOrderEventPayload(ev),
            ev.getPayment().getPaymentStatus(),
            OutboxStatus.STARTED,
            UUID.fromString(req.getSagaId()));
    }
}
```

### Why this matters

- The **idempotency check** (outbox lookup) and the **DB writes** must share one transaction so a duplicate Kafka delivery becomes a no-op.
- The listener stays free of `@Transactional` so framework exceptions (Kafka deserialization, batch handling, optimistic-lock retries) never accidentally roll back domain work.
- A saga step (`SagaStep.process / rollback`) is itself the helper-equivalent: it is `@Transactional` and is invoked by an even thinner `*MessageListener` impl that just dispatches to `process` or `rollback` based on payload status.

## Note on `ApplicationEventDomainPublisher` (vestigial)

`order-service` ships `ApplicationEventDomainPublisher implements DomainEventPublisher<OrderCreatedEvent>, ApplicationEventPublisherAware`. **Nothing in the codebase listens to the event** (no `@TransactionalEventListener` / `@EventListener` anywhere). The outbox row is written **synchronously** inside `OrderCreateCommandHandler.createOrder` immediately after `persistOrder` returns. Treat the bridge class as dead code — **do not copy it** into a new service unless you also add an explicit `@TransactionalEventListener` consumer that writes the outbox.

## Outbox helper beans

To keep the saga readable, factor outbox CRUD into `<Topic>OutboxHelper` `@Component`s:

```java
@Component
@RequiredArgsConstructor
public class PaymentOutboxHelper {
    private final PaymentOutboxRepository repo;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public Optional<OrderPaymentOutboxMessage> getPaymentOutboxMessageBySagaIdAndSagaStatus(
            final UUID sagaId, final SagaStatus... statuses) { … }

    @Transactional
    public void save(final OrderPaymentOutboxMessage msg) { repo.save(msg); }

    @Transactional
    public void savePaymentOutboxMessage(
            final OrderPaymentEventPayload payload,
            final OrderStatus orderStatus,
            final SagaStatus sagaStatus,
            final OutboxStatus outboxStatus,
            final UUID sagaId) { … }
}
```

## End-to-end flow (Order service paying an order)

```
[OrderApplicationService.createOrder]
   │  @Transactional
   ├─ domain: orderDomainService.validateAndInitiateOrder(...)
   ├─ orderRepository.save(order)
   └─ paymentOutboxHelper.savePaymentOutboxMessage(payload, sagaStatus=STARTED, outboxStatus=STARTED)
                          │
                          ▼
[PaymentOutboxScheduler @Scheduled]
   - reads outbox where outboxStatus=STARTED AND sagaStatus IN (STARTED, COMPENSATING)
   - paymentRequestMessagePublisher.publish(msg, this::updateOutboxStatus)
                          │
                          ▼  ack callback → outboxStatus=COMPLETED
[Kafka topic: payment-request]
                          │
                          ▼
[Payment service consumes, processes, emits payment-response]
                          │
                          ▼
[Order service: PaymentResponseKafkaListener (batch, NO-OP on OptimisticLockingFailure)]
                          │
                          ▼
[OrderPaymentSaga.process(PaymentResponse)]
   @Transactional
   - find outbox by (sagaId, SagaStatus.STARTED) — return if absent
   - apply OrderPaidEvent on aggregate
   - paymentOutboxHelper.save(updated → SagaStatus.PROCESSING)
   - approvalOutboxHelper.save(new approval payload, SagaStatus.PROCESSING, OutboxStatus.STARTED)
                          │
                          ▼
[ApprovalOutboxScheduler] → restaurant-approval-request → … (saga continues)
```

## Listener wiring

The listener **decides** which saga method to call based on the inbound payload status:

```java
@Override
public void paymentCompleted(final PaymentResponse response) {
    orderPaymentSaga.process(response);
}

@Override
public void paymentCancelled(final PaymentResponse response) {
    orderPaymentSaga.rollback(response);
}
```

## Testing a saga

- **Unit**: mock the helpers + domain service; call `process()` and assert outbox interactions.
- **Integration** (`Lg5TestBoot`): real DB via Testcontainers, mock Kafka publisher, assert outbox rows after invocation.
- **ATDD** (`Lg5TestBootPortNone` + Cucumber): fire a Kafka event into the running container, poll DB or downstream topic for assertions.

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Call the next saga step directly | Insert outbox row, let scheduler relay |
| Skip the `(sagaId, status)` lookup | Always query first → idempotent |
| Use `@Async` on saga methods | Stay synchronous within `@Transactional` |
| Split aggregate save and outbox save in two transactions | One `@Transactional` boundary |
| Throw on duplicate processing | Log and return |
| Put business decisions in the listener | Listener delegates to saga; saga decides |
