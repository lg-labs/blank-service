---
name: food-ordering-system
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: Canonical reference implementation breakdown of the food-ordering-system project (https://github.com/lg-labs/food-ordering-system) — 4 microservices on top of lg5-spring with full Saga/Outbox orchestration. Use this skill when the user asks "how does food-ordering-system do X", needs to copy a real-world pattern, wants to trace the order saga end-to-end, or needs concrete examples of orchestrator vs participant services, reverse outbox, or multi-service ATDD.
---

# food-ordering-system — Reference Implementation Breakdown

> Repo on disk: `/tmp/lg5-study/food-ordering-system/`
> Maven groupId: `com.labs.lg.food.ordering.system` · version: `1.0.0-alpha`
> Inherits `com.lg5.spring:lg5-spring-parent:1.0.0-alpha.d0d754a`
> 4 services, 5 Kafka topics, 4 outbox tables, 1 orchestrator (order-service).

## 1. The 4 services at a glance

| Service | Port | Schema | REST | Saga role | Outbox tables |
|---|---|---|---|---|---|
| `customer-service` | 8184 | `customer` | `POST /customers` | none — direct fire-and-forget Kafka | none |
| `order-service` | 8181 | `"order"` (quoted, reserved) | `POST /orders`, `GET /orders/{trackingId}` | **orchestrator** | `payment_outbox`, `restaurant_approval_outbox` |
| `payment-service` | 8182 | `payment` | none (Kafka-only) | participant | `order_outbox` (reverse) |
| `restaurant-service` | 8183 | `restaurant` | none (Kafka-only) | participant | `order_outbox` (reverse) |

Module shape per service mirrors `blank-service`: `<svc>-api`, `<svc>-domain/{<svc>-domain-core, <svc>-application-service}`, `<svc>-data-access`, `<svc>-message/{<svc>-message-core, <svc>-message-model}`, `<svc>-container`, `<svc>-acceptance-test`, `<svc>-support`.

## 2. Topic ↔ outbox matrix

5 topics, all `partitions=3 RF=3` (declared in `infrastructure/docker-compose/init_kafka.yml`):

| Topic | Producer | Consumer | Avro model | Drives |
|---|---|---|---|---|
| `customer` | customer-service (no outbox) | order-service `CustomerKafkaListener` | `CustomerAvroModel` | local replication |
| `payment-request` | order-service `OrderPaymentEventKafkaPublisher` | payment-service `PaymentRequestKafkaMessageListener` | `PaymentRequestAvroModel` | saga step 1 fwd |
| `payment-response` | payment-service `PaymentEventKafkaPublisher` | order-service `PaymentResponseKafkaListener` | `PaymentResponseAvroModel` | saga step 1 ack |
| `restaurant-approval-request` | order-service `OrderApprovalEventKafkaPublisher` | restaurant-service `RestaurantApprovalRequestKafkaListener` | `RestaurantApprovalRequestAvroModel` | saga step 2 fwd |
| `restaurant-approval-response` | restaurant-service `RestaurantApprovalEventKafkaPublisher` | order-service `RestaurantApprovalResponseKafkaListener` | `RestaurantApprovalResponseAvroModel` | saga step 2 ack |

Outbox asymmetry:

| Outbox | Owner | Direction | `SagaStatus` col? | Why |
|---|---|---|---|---|
| `"order".payment_outbox` | order-service | outbound | YES | orchestrator needs `(sagaId, SagaStatus.STARTED)` lookup |
| `"order".restaurant_approval_outbox` | order-service | outbound | YES | same |
| `payment.order_outbox` | payment-service | reverse (response) | NO | participant only needs `OutboxStatus` |
| `restaurant.order_outbox` | restaurant-service | reverse (response) | NO | same |

## 3. End-to-end happy path (Order → Payment → Approval → PAID)

```
Client → POST /orders (vnd.api.v1+json)
   └─ OrderController → OrderApplicationService → OrderCreateCommandHandler.createOrder
      @Transactional {
        OrderCreateHelper.persistOrder
          ├─ orderRepository.save(order)           # Order aggregate, status=PENDING
          └─ ApplicationEventDomainPublisher
               (DomainEvent → Spring ApplicationEvent bridge)
               → OrderCreatedEvent listener
                  → PaymentOutboxHelper.savePaymentOutboxMessage
                       (OutboxStatus.STARTED, SagaStatus.STARTED, OrderStatus.PENDING)
      }
   ← 201 { trackingId }

⏱ PaymentOutboxScheduler @Scheduled(fixedDelayString="${order-service.outbox-scheduler-fixed-rate}"=10000ms)
   - findByTypeAndOutboxStatusAndSagaStatusIn(STARTED, [STARTED, COMPENSATING])
   - OrderPaymentEventKafkaPublisher.publish(msg, callback)
        kafkaProducer.send("payment-request", sagaId, PaymentRequestAvroModel, callback)
        callback on ack → PaymentOutboxHelper update OutboxStatus.COMPLETED

[payment-request topic]
  payment-service PaymentRequestKafkaMessageListener (batch, KafkaConsumer<PaymentRequestAvroModel>)
    → PaymentRequestMessageListenerImpl.completePayment(PaymentRequest)
      → PaymentRequestHelper.persistPayment @Transactional {
          PaymentDomainServiceImpl.validateAndInitiatePayment
            - validateCreditEntry(price ≤ available)
            - subtractCreditEntry, addCreditHistory(DEBIT)
            - emit PaymentCompletedEvent (or PaymentFailedEvent)
          paymentRepository.save(payment)
          OrderOutboxHelper.saveOrderOutboxMessage
            (OutboxStatus.STARTED, OrderEventPayload{paymentStatus=COMPLETED})
        }
  ⏱ OrderOutboxScheduler → PaymentEventKafkaPublisher → "payment-response"

[payment-response topic]
  order-service PaymentResponseKafkaListener (batch)
    swallows OptimisticLockingFailureException + OrderNotFoundException → NO-OP
    if PaymentStatus.COMPLETED → paymentResponseMessageListener.paymentCompleted
       → OrderPaymentSaga.process(PaymentResponse) @Transactional {
            outbox = paymentOutboxHelper.getBySagaIdAndSagaStatus(sagaId, STARTED)
            if (outbox.isEmpty) return                     ← idempotency guard
            order = orderSagaHelper.findOrder(orderId)
            order.pay()                                    ← domain mutation
            paymentOutboxHelper.save(outbox→SUCCEEDED, PAID)
            approvalOutboxHelper.saveApprovalOutboxMessage
               (STARTED, OrderApprovalEventPayload, PAID)
          }

⏱ RestaurantApprovalOutboxScheduler → OrderApprovalEventKafkaPublisher → "restaurant-approval-request"

[restaurant-approval-request topic]
  restaurant-service RestaurantApprovalRequestKafkaListener
    → RestaurantApprovalRequestMessageListenerImpl.approveOrder
      → RestaurantApprovalRequestHelper.persistOrderApproved @Transactional {
          Restaurant restaurant = restaurantRepository.findRestaurantInformation(...)
          // Domain rule lives in the aggregate:
          restaurantDomainServiceImpl.validateOrder(restaurant, failureMessages)
            - Restaurant.validateOrder():
                * orderStatus must be PAID
                * each Product.isAvailable()
                * sum(items.price) == orderDetail.totalAmount
            - emits OrderApprovedEvent OR OrderRejectedEvent
          OrderOutboxHelper.saveOrderOutboxMessage(STARTED, OrderEventPayload{approvalStatus, failureMessages})
        }
  ⏱ OrderOutboxScheduler → RestaurantApprovalEventKafkaPublisher → "restaurant-approval-response"

[restaurant-approval-response topic]
  order-service RestaurantApprovalResponseKafkaListener (batch)
    if status APPROVED → orderApproved → OrderApprovalSaga.process @Transactional {
       outbox = approvalOutboxHelper.get(sagaId, STARTED)
       if empty return
       order.approve()                                     ← order.status = APPROVED
       approvalOutboxHelper.save(SUCCEEDED, APPROVED)
    }
    if status REJECTED → orderRejected → OrderApprovalSaga.rollback (see §4)
```

## 4. Compensation paths

### Payment fails (saga step 1 negative)

```
payment-response (PaymentStatus.CANCELLED|FAILED)
 → PaymentResponseKafkaListener → paymentCancelled / paymentFailed
   → OrderPaymentSaga.rollback @Transactional {
       outbox = paymentOutboxHelper.get(sagaId, COMPENSATING) ← note status
       if empty return
       order.cancel(failureMessages)                       ← order.status = CANCELLED
       paymentOutboxHelper.save(SUCCEEDED|COMPENSATED, CANCELLED)
     }
```

### Approval fails (saga step 2 negative — refund flow)

```
restaurant-approval-response (RestaurantOrderStatus.REJECTED)
 → RestaurantApprovalResponseKafkaListener.orderRejected
   → OrderApprovalSaga.rollback @Transactional {
       outbox = approvalOutboxHelper.get(sagaId, STARTED)
       if empty return
       order.initCancel(failureMessages)                   ← order.status = CANCELLING
       approvalOutboxHelper.save(COMPENSATING, CANCELLING)
       // Re-arms payment leg in REVERSE direction:
       paymentOutboxHelper.savePaymentOutboxMessage
         (OutboxStatus.STARTED, SagaStatus.COMPENSATING,
          OrderStatus.CANCELLING, paymentOrderStatus=CANCELLED)
     }

⏱ PaymentOutboxScheduler picks STARTED+COMPENSATING → publishes refund "payment-request"

[payment-request, paymentOrderStatus=CANCELLED]
  payment-service PaymentRequestMessageListenerImpl.cancelPayment
    → PaymentDomainServiceImpl.validateAndCancelPayment
       - addCreditEntry(refund), addCreditHistory(CREDIT)
       - emit PaymentCancelledEvent
    → OrderOutboxHelper.saveOrderOutboxMessage(STARTED, paymentStatus=CANCELLED)
  → "payment-response" (CANCELLED)

[payment-response]
  order-service OrderPaymentSaga.rollback (second time, now in COMPENSATING state)
    order.cancel(failureMessages) → status = CANCELLED
    paymentOutboxHelper.save(SUCCEEDED, CANCELLED)
```

The same `PaymentRequestKafkaMessageListener` is reused for both PAY and REFUND — discriminated by the `paymentOrderStatus` field on the Avro model (`PENDING|CANCELLED`).

## 5. order-service deep dive (the orchestrator)

### 5.1 Use-case slicing

```
OrderApplicationServiceImpl   ← input port impl, just delegates to handlers
   ├─ OrderCreateCommandHandler.createOrder        (POST /orders)
   │   └─ OrderCreateHelper.persistOrder           @Transactional
   │       ├─ orderRepository.save(order)
   │       └─ ApplicationEventDomainPublisher.publish(OrderCreatedEvent)
   │           → @TransactionalEventListener picks it up → PaymentOutboxHelper.save…
   └─ OrderTrackCommandHandler.trackOrder          (GET /orders/{trackingId})
```

### 5.2 The two SagaStep beans

Both `@Component`, both implement `com.lg5.spring.saga.SagaStep<T>`, both `@Transactional` on `process` and `rollback`:

| Saga | T | process effect | rollback effect |
|---|---|---|---|
| `OrderPaymentSaga` | `PaymentResponse` | `order.pay()` + payment_outbox(SUCCEEDED, PAID) + restaurant_approval_outbox(STARTED, PAID) | `order.cancel()` + payment_outbox(COMPENSATED, CANCELLED) |
| `OrderApprovalSaga` | `RestaurantApprovalResponse` | `order.approve()` + restaurant_approval_outbox(SUCCEEDED, APPROVED) | `order.initCancel()` + restaurant_approval_outbox(COMPENSATING, CANCELLING) + payment_outbox(STARTED, CANCELLING) |

### 5.3 Helper beans

Helpers absorb the `@Transactional` boundary so listeners/handlers stay thin:

```
OrderCreateHelper          - persistOrder, build order from CreateOrderCommand
OrderSagaHelper            - findOrder(orderId), orderStatusToSagaStatus mapping
PaymentOutboxHelper        - get/save/savePaymentOutboxMessage/delete by status
ApprovalOutboxHelper       - same, for restaurant_approval_outbox
CustomerHelper             - persists Customer locally on CustomerKafkaListener events
```

`PaymentOutboxHelper` (representative API):

```java
@Component @Slf4j @RequiredArgsConstructor
public class PaymentOutboxHelper {
    private final PaymentOutboxRepository repo;
    private final ObjectMapper mapper;
    private static final String SAGA = "OrderProcessingSaga";

    @Transactional(readOnly = true)
    Optional<OrderPaymentOutboxMessage> getPaymentOutboxMessageBySagaIdAndSagaStatus(
        UUID sagaId, SagaStatus... statuses);

    @Transactional(readOnly = true)
    Optional<List<OrderPaymentOutboxMessage>> getPaymentOutboxMessageByOutboxStatusAndSagaStatus(
        OutboxStatus outboxStatus, SagaStatus... statuses);

    @Transactional
    void save(OrderPaymentOutboxMessage msg);

    @Transactional
    void savePaymentOutboxMessage(OrderPaymentEventPayload payload, OrderStatus orderStatus,
                                  SagaStatus sagaStatus, OutboxStatus outboxStatus, UUID sagaId);

    @Transactional
    void deletePaymentOutboxMessageByOutboxStatusAndSagaStatus(
        OutboxStatus outboxStatus, SagaStatus... sagaStatuses);
}
```

### 5.4 The `ApplicationEventDomainPublisher` bridge

This is the cleanest pattern in the repo for keeping the domain Spring-free:

```java
// order-application-service/.../ApplicationEventDomainPublisher.java
@Component
public class ApplicationEventDomainPublisher
        implements DomainEventPublisher<OrderCreatedEvent>, ApplicationEventPublisherAware {

    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(OrderCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
        log.info("OrderCreatedEvent published");
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.applicationEventPublisher = publisher;
    }
}
```

Then a regular Spring `@EventListener` (or `@TransactionalEventListener(phase = AFTER_COMMIT)`) inside the application-service module subscribes to `OrderCreatedEvent` and writes the outbox row. The domain only knows `DomainEventPublisher<T>` from `lg5-common-domain`, no Spring import.

### 5.5 Schedulers (4 of them)

```
PaymentOutboxScheduler              fixedDelayString="${order-service.outbox-scheduler-fixed-rate}" (10s)
PaymentOutBoxCleanerScheduler       cron="@midnight"
RestaurantApprovalOutboxScheduler   fixedDelayString=…
RestaurantApprovalOutboxCleanerScheduler cron="@midnight"
```

All `implements OutboxScheduler` (framework iface). All gated by:

```java
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
```

Cleaners delete `outbox_status = COMPLETED AND saga_status IN (SUCCEEDED, FAILED, COMPENSATED)`.

### 5.6 OrderServiceApplication scan config

```java
@SpringBootApplication(scanBasePackages = {
    "com.labs.lg.food.ordering.system",
    "com.lg5.spring.kafka",      // pulls framework KafkaProducer/Consumer beans
    "com.lg5.spring.outbox"      // pulls OutboxScheduler infrastructure
})
@EnableJpaRepositories(basePackages = {
    "com.labs.lg.food.ordering.system.order.service.dataaccess",
    "com.labs.lg.food.ordering.system.dataaccess"
})
@EntityScan(basePackages = { ...same as above })
public class OrderServiceApplication { … }
```

`BeanConfiguration` only declares the framework-free `OrderDomainService` bean.

### 5.7 Order domain methods (the aggregate API)

`order-domain-core/.../entity/Order.java`:

```java
public class Order extends AggregateRoot<OrderId> {
    public void validateOrder();      // business invariants
    public void initializeOrder();    // sets id, trackingId, status=PENDING, items ids
    public void pay();                // requires PENDING → status=PAID
    public void approve();            // requires PAID → status=APPROVED
    public void initCancel(List<String> failureMessages);   // PAID → CANCELLING
    public void cancel(List<String> failureMessages);       // CANCELLING|PENDING → CANCELLED
}
```

Each method enforces the state machine via guard exceptions. Saga steps call exactly one of these per invocation.

## 6. payment-service deep dive (participant + reverse outbox)

### 6.1 Module layout

```
payment-domain-core/
   entity/{Payment, CreditEntry, CreditHistory}
   PaymentDomainServiceImpl
       validateAndInitiatePayment(payment, creditEntry, creditHistory, failureMessages)
       validateAndCancelPayment(payment, creditEntry, creditHistory, failureMessages)
   event/{PaymentCompletedEvent, PaymentCancelledEvent, PaymentFailedEvent}

payment-application-service/
   PaymentRequestMessageListenerImpl   (input port: completePayment, cancelPayment)
   PaymentRequestHelper                @Transactional persistPayment
   outbox/scheduler/{OrderOutboxHelper, OrderOutboxScheduler, OrderOutboxCleanerScheduler}
   outbox/model/OrderEventPayload      record(paymentId, orderId, customerId, price, createdAt,
                                               paymentStatus, failureMessages)
```

### 6.2 The reverse outbox

`payment.order_outbox` columns:

```
id            uuid PK
saga_id       uuid                      # correlation only
created_at    timestamp with tz
processed_at  timestamp with tz
type          varchar                   # = "OrderProcessingSaga"
payload       jsonb                     # OrderEventPayload
outbox_status varchar (enum)            # STARTED | COMPLETED | FAILED — no SagaStatus
version       int @Version
```

The participant doesn't run the saga — it just writes a response and lets the relay scheduler push it to Kafka. Cleaner deletes `outbox_status = COMPLETED` (no SagaStatus filter).

### 6.3 `PaymentRequestHelper.persistPayment` (representative)

```java
@Transactional
public void persistPayment(PaymentRequest paymentRequest) {
    Payment payment = paymentDataMapper.paymentRequestModelToPayment(paymentRequest);
    CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
    List<CreditHistory> creditHistories = getCreditHistories(payment.getCustomerId());
    List<String> failureMessages = new ArrayList<>();

    PaymentEvent event = paymentDomainService.validateAndInitiatePayment(
        payment, creditEntry, creditHistories, failureMessages);

    persistDbObjects(payment, creditEntry, creditHistories, failureMessages);

    orderOutboxHelper.saveOrderOutboxMessage(
        paymentDataMapper.paymentEventToOrderEventPayload(event),
        event.getPayment().getPaymentStatus(),
        OutboxStatus.STARTED,
        UUID.fromString(paymentRequest.getSagaId()));
}
```

`cancelPayment` is symmetric, calls `validateAndCancelPayment` and persists with `paymentStatus=CANCELLED`.

## 7. restaurant-service deep dive (participant)

Symmetric to payment-service. Differences:

- The domain rule **lives in the aggregate** (`Restaurant.validateOrder(failureMessages)` in `restaurant-domain-core/.../entity/Restaurant.java`):
  - `OrderStatus` must be `PAID`
  - each `Product.isAvailable()`
  - recompute total via `Money::add`, compare to `orderDetail.totalAmount`
- `RestaurantDomainServiceImpl.validateOrder` returns either `OrderApprovedEvent` or `OrderRejectedEvent`.
- `RestaurantApprovalRequestHelper.persistOrderApproved` is the single `@Transactional` write entry.
- `restaurant.order_outbox` payload includes `failureMessages` + `orderApprovalStatus`.

## 8. customer-service (the odd one out — no outbox)

```
customer-api          POST /customers (vnd.api.v1+json)

customer-application-service
   CustomerCreateCommandHandler.createCustomer @Transactional
       customerRepository.save(customer) → returns CustomerCreatedEvent
   CustomerApplicationServiceImpl.createCustomer
       handler.createCustomer(cmd)
       customerMessagePublisher.publish(event)        ← DIRECT publish, NO outbox

customer-message-core
   CustomerEventKafkaPublisher implements CustomerMessagePublisher
       kafkaProducer.send("customer", customerId, CustomerAvroModel, callback)
```

**Why no outbox**: customer creation is not part of any saga. It's a one-shot data-replication broadcast — order-service keeps a local read model. If publish fails, the user retries the REST call. The framework's outbox is reserved for cross-aggregate saga state.

This proves: **outbox is mandatory only for saga participants/orchestrators**, not for every Kafka producer.

## 9. Cross-cutting patterns (copy verbatim into new services)

1. **Helper class per use-case**. Listeners, command handlers, and sagas are *thin*. The `@Transactional` boundary lives in a sibling `*Helper`. Pattern: `XxxMessageListenerImpl` → `XxxHelper.persistXxx(@Transactional)` → repository + outbox in the same Tx.

2. **DomainEventPublisher → Spring bridge**. `ApplicationEventDomainPublisher implements DomainEventPublisher<E>, ApplicationEventPublisherAware`. Domain stays Spring-free; bridge in app-service.

3. **Idempotency guard** at the top of every `SagaStep.process`/`rollback`:
   ```java
   if (outboxHelper.getBySagaIdAndSagaStatus(sagaId, EXPECTED_STATUS).isEmpty()) return;
   ```

4. **Outbox JPA entity invariants**: `@Id UUID id`, `UUID sagaId`, `ZonedDateTime createdAt/processedAt`, `String type`, `String payload` (jsonb), `OutboxStatus`, `@Version int version`. Add `SagaStatus` + per-aggregate status enum if you're the orchestrator.

5. **Payload class lives in app-service** module under `outbox/model/`, named `<Context>EventPayload`, JSON-serialized into outbox. **Different class from the domain event**.

6. **Two schedulers per outbox table**: poller (`fixedDelayString`) + cleaner (`@Scheduled(cron="@midnight")`). Both gated by `scheduling.enabled`.

7. **Kafka producer callback updates outbox**. The `BiConsumer<SendResult, Throwable>` flips outbox row to `COMPLETED`/`FAILED`. **Kafka key = `sagaId`** (always).

8. **Kafka consumer = batch**. Implement `com.lg5.spring.kafka.consumer.KafkaConsumer<TAvroModel>`; receive `List<T>` + `List<String> keys` + `List<Integer> partitions` + `List<Long> offsets`. Catch `OptimisticLockingFailureException` + not-found → NO-OP.

9. **MessagingDataMapper** in `*-message-core/.../mapper/` translates **Avro ↔ application-service DTO** (NOT domain).

10. **DataAccessMapper** in `*-data-access/.../mapper/` translates **JPA entity ↔ domain aggregate**.

11. **`*RepositoryImpl` adapter pattern**: a `@Component` in `*-data-access/.../adapter/` implements the output port (`OrderRepository`), injects `*JpaRepository` + `*DataAccessMapper`, hides JPA from the application-service.

12. **Postgres-native enums** + `@Enumerated(EnumType.STRING)`. DDL: `CREATE TYPE order_status AS ENUM(...)`. Hibernate maps the varchar representation to the Postgres enum.

13. **`payload jsonb`** for outbox — never `text`/`varchar`.

14. **Schema-per-service** in shared Postgres (`postgres` DB): connection URL `jdbc:postgresql://localhost:5432/postgres?currentSchema=<svc>&...`. Liquibase quotes `"order"` (reserved word).

15. **`SagaConstants.ORDER_SAGA_NAME = "OrderProcessingSaga"`** is duplicated verbatim in each service. Smell — but consistent.

## 10. Liquibase migrations (order-service example)

`order-data-access/src/main/resources/db/changelog/`:

```
db.changelog-master.yaml         (includes all ddl-order-v.0.0.*.yaml)
ddl-order-v.0.0.1.yaml           Postgres extensions (uuid-ossp, btree_gist)
ddl-order-v.0.0.2.yaml           order/ schema, order/restaurant/customer tables
ddl-order-v.0.0.3.yaml           order_items
ddl-order-v.0.0.4.yaml           Postgres ENUMs: order_status, saga_status, outbox_status
ddl-order-v.0.0.5.yaml           grants and indexes on orders
ddl-order-v.0.0.6.yaml           payment_outbox table + indexes (incl. unique on (type, sagaId, sagaStatus))
ddl-order-v.0.0.7.yaml           restaurant_approval_outbox table + indexes
ddl-order-v.0.0.8.yaml           triggers / housekeeping
```

Mandatory outbox indexes (CRITICAL for scheduler performance):

```sql
CREATE INDEX payment_outbox_outbox_status_saga_status
    ON "order".payment_outbox (type, outbox_status, saga_status);
CREATE UNIQUE INDEX payment_outbox_saga_id_saga_status
    ON "order".payment_outbox (type, saga_id, saga_status);
```

## 11. Container modules — shared anatomy

Every `*-container` has:

| File | Purpose |
|---|---|
| `<Svc>ServiceApplication.java` | `@SpringBootApplication(scanBasePackages = { "...", "com.lg5.spring.kafka", "com.lg5.spring.outbox" })` + `@EnableJpaRepositories` + `@EntityScan` |
| `BeanConfiguration.java` | Declares the (Spring-free) `<Svc>DomainService` bean |
| `application.yaml` | server port + datasource + kafka-config + kafka-producer-config + kafka-consumer-config + `<svc>-service.outbox-scheduler-*` |
| `bootstrap.yaml` | only `spring.application.name` (Spring Cloud bootstrap) |
| `logback-spring.xml` | identical 196-line template across services; binds `springProperty` for log levels and APP_NAME, includes `org/springframework/boot/logging/logback/defaults.xml` |

## 12. Acceptance test pattern (`<svc>-acceptance-test/`)

```
src/test/java/.../boot/
   AcceptanceTestCase.java            JUnit5 @Suite + @IncludeEngines("cucumber")
                                      classpath:features  glue=...service
   CucumberHooks.java                 extends Lg5TestBootPortNone
                                      @CucumberContextConfiguration
                                      @Import(TestContainersLoader.class)
   TestContainersLoader.java          @Configuration
                                      @Import({Postgres,Kafka,Wiremock,App}ContainerCustomConfig.class)
                                      @Bean apiContainer (extends app container)
src/test/java/.../steps/
   OrderServiceSteps.java             @When/@Then glue (mostly TODO stubs)
   PaymentServiceSteps.java
   RestaurantServiceSteps.java
src/test/resources/
   application-test.yml               application.image.name=…/order-service:1.0.0-alpha
                                      application.image.port=8080
                                      application.traces.{console,file}.enabled
                                      wiremock.placeholder.template=wiremock/placeholder/template.json
                                      third.api.url=https://jsonplaceholder.typicode.com
                                      testcontainers.{postgres,kafka,wiremock,app}.enabled=true
   features/order-service.feature
   wiremock/placeholder/template.json (third-system stub)
```

Multi-service ATDD does NOT spin up payment-service + restaurant-service. Instead the test fires Kafka events directly via the test KafkaTemplate and asserts side-effects (DB rows, outbox transitions, downstream Kafka publishes).

## 13. Container-module integration tests (`<svc>-container/src/test/`)

Different flavor from ATDD — full Spring context, random port:

```
boot/
   Bootstrap.java                          (extends Lg5TestBoot — RANDOM_PORT, RestAssured)
   TestContainersLoader.java               (subset of the ATDD loader; no AppContainer)
   TestOrderServiceApplication.java
       SpringApplication.from(OrderServiceApplication::main).with(...)
data/OrdersIT.java                         JPA repository sanity
api/rest/HealthCheckIT.java                /actuator/health
outbox/OrderPaymentSagaIT.java             FULL saga IT:
   @Sql(scripts="/sql/OrderPaymentSagaTestSetUp.sql", BEFORE_TEST_METHOD)
   @Sql(scripts="/sql/OrderPaymentSagaTestCleanUp.sql", AFTER_TEST_METHOD)
   - POST /orders via RestAssured
   - send fake PaymentResponseAvroModel via test KafkaTemplate
   - Awaitility.until(repo.findByTrackingIdAndStatus(...).isPresent())
```

The `sql/*.sql` setup pattern bootstraps reference data (customer + restaurant + products) before each test and wipes outbox tables after.

## 14. Infrastructure (`infrastructure/docker-compose/`)

| File | Role |
|---|---|
| `common.yml` | shared `lg-labs-food-ordering-system` bridge network |
| `zookeeper.yml` | single ZK node |
| `kafka_cluster.yml` | 3 brokers (host ports 19092/29092/39092) + schema-registry on 8081 |
| `init_kafka.yml` | one-shot container, creates the 5 topics with `--partitions 3 --replication-factor 3` |
| `kafka_mngr.yml` | yahoo Kafka Manager UI |
| `kafka-ui/docker-compose.yml` | provectus/kafka-ui |
| `docker-compose-ddbb.yml` | `postgres:16.0-alpine` (`lglabs/lgpass`, port 5432) + `dpage/pgadmin4:8.6` on 5011 |
| `filebeats/`, `log-manager/` | log shipping & ELK |

Make targets (`/tmp/lg5-study/food-ordering-system/Makefile`):

```
zookeeper-up / -down
kafka-cluster-up / -down
kafka-init-up
kafka-mngr-up
ddbb-up / -down
docker-up         # composes everything in order
docker-down
run-customer / run-order / run-payment / run-restaurant   spring-boot:run per service
run-apps          # all four in parallel
run-happy-path    # docker-down → docker-up → run-apps
install / install-skip-test
build_to_arm / build_to_amd                                 Jib platform overrides
run-checkstyle / run-verify
run-acceptance-test / run-atdd-module ATDD=…
run-at-by-tag TAG_NAME=@smoke
run-test-spec TEST_NAME=OrderPaymentSagaIT
run-kafka-model    # regen Avro classes
```

## 15. Root pom + CI/CD

- Parent: `com.lg5.spring:lg5-spring-parent:1.0.0-alpha.d0d754a` (single source of versions).
- `<dependencyManagement>` enumerates every `<svc>-*` module — services declare deps without versions.
- Plugins: `maven-compiler-plugin` (`<release>21</release>`), `maven-checkstyle-plugin` (config in parent), Jib via parent.

CI (`.github/workflows/c-integration.yml`):
- Matrix `[customer, order, payment, restaurant]`, JDK 21 (Zulu).
- Steps: setup → checkstyle → coverage → quality → build (Jib produces `.tar` artifact) → test (ATDD; only `customer` matrix today) → visualization (gource).
- `settings.xml` injects a `github` profile pointing to `https://maven.pkg.github.com/lg-labs-pentagon/*` (uses `secrets.PKG_GITHUB_TOKEN`).
- Coverage uploads `target/site/jacoco-aggregate-all` (lives in `<svc>-support/target/site`).

CD (`c-delivery.yaml`):
- Loads CI artifact, tags as `ghcr.io/${repo}/${IMAGE_NAME}`, pushes to GitHub Packages.
- Looks up previous version via Packages REST API and **deletes the old image**.

## 16. Gotchas / smells

- **`fixedDelay` vs `fixedRate` inconsistency** between order-service (`fixedDelayString`) and payment/restaurant schedulers (`fixedRateString`). Likely accidental — copy `fixedDelayString` for new services.
- **`SagaConstants.ORDER_SAGA_NAME` duplication** in every service. Centralize in a shared module if the saga grows beyond 3 participants.
- **`fooo-ordering-system` does NOT spin up real payment+restaurant in order ATDD** — it fakes responses via test KafkaTemplate. True end-to-end requires `make run-happy-path`.
- **`outbox.type` column always equals saga name** — leaves the door open for a single outbox table shared by multiple sagas, but this repo splits by leg.
- **DDD building blocks** (`AggregateRoot`, `BaseEntity`, `BaseId`, `Money`, `DomainEvent`) come from `com.labs.lg.pentagon:ddd-common-domain` (re-exported by `lg5-common-domain`) — confirmed by `Restaurant.java` import: `com.labs.lg.pentagon.common.domain.entity.AggregateRoot`.

## 17. Quick navigation map (file:role)

| File | Role |
|---|---|
| `order-service/order-domain/order-application-service/.../OrderPaymentSaga.java` | First saga step (forward + compensation) |
| `order-service/order-domain/order-application-service/.../OrderApprovalSaga.java` | Second saga step + refund chaining |
| `order-service/order-domain/order-application-service/.../ApplicationEventDomainPublisher.java` | DomainEvent → Spring ApplicationEvent bridge |
| `order-service/order-domain/order-application-service/.../outbox/scheduler/payment/PaymentOutboxScheduler.java` | Outbox relay |
| `order-service/order-data-access/.../outbox/payment/entity/PaymentOutboxEntity.java` | Outbox JPA entity (`@Version`) |
| `order-service/order-message/order-message-core/.../publisher/kafka/OrderPaymentEventKafkaPublisher.java` | Kafka producer adapter |
| `order-service/order-message/order-message-core/.../listener/kafka/PaymentResponseKafkaListener.java` | Batch Kafka consumer (NO-OP exception swallow) |
| `order-service/order-message/order-message-model/src/main/resources/avro/payment_request.avsc` | Avro schema |
| `order-service/order-container/src/main/resources/application.yaml` | Full kafka-config + kafka-*-config sample |
| `order-service/order-container/src/test/java/.../outbox/OrderPaymentSagaIT.java` | Container-level saga IT with `@Sql` setup |
| `order-service/order-acceptance-test/src/test/java/.../boot/TestContainersLoader.java` | ATDD container wiring |
| `payment-service/payment-domain/payment-application-service/.../PaymentRequestHelper.java` | Reverse-outbox participant pattern |
| `restaurant-service/restaurant-domain/restaurant-domain-core/.../entity/Restaurant.java` | Domain rule lives in the aggregate (`validateOrder`) |
| `customer-service/customer-domain/customer-application-service/.../CustomerApplicationServiceImpl.java` | No-outbox, direct-publish pattern |
| `infrastructure/docker-compose/init_kafka.yml` | All 5 Kafka topics |
| `Makefile` | Every dev workflow target |
| `pom.xml` | Parent + `<dependencyManagement>` with all `<svc>-*` modules |
| `.github/workflows/c-integration.yml` | CI matrix |
