---
id: RULE-012
slug: test-profiles
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: testing
tags: [testing, profiles, lg5-test-boot, integration-tests, atdd]
description: Integration & ATDD tests always run with `@ActiveProfiles({"test","local"})` and extend `Lg5TestBoot` (random port + RestAssured) or `Lg5TestBootPortNone` (NONE web env).
---

# RULE-012 — Test profile activation and base classes

## Statement

Every integration test (`*IT.java` in `*-container`) and every ATDD test (in
`*-acceptance-test`) must:

1. Run with `@ActiveProfiles({"test","local"})` — exactly these two profiles,
   in this order. `test` provides the test-specific overrides
   (`application-test.yaml`); `local` provides the local-env defaults that
   real-mode services would inherit (`application-local.yaml`).
2. Extend exactly one of:
   - `com.lg5.spring.test.Lg5TestBoot` — for tests that need a running web
     server, RestAssured-driven HTTP assertions, and a random `port` (`@LocalServerPort`).
   - `com.lg5.spring.test.Lg5TestBootPortNone` — for tests with
     `WebEnvironment.NONE` (saga/outbox/JPA-only IT, no REST surface).
3. Never hand-roll the `@SpringBootTest(classes = ..., webEnvironment = ...)`
   declaration — let the base class own that.

## Rationale

The two base classes encode the framework's testing posture in one place:

- `Lg5TestBoot` configures `@SpringBootTest(webEnvironment = RANDOM_PORT)`,
  wires RestAssured to the random port, and brings in the standard test
  utilities (mocks, fixtures, JSON helpers).
- `Lg5TestBootPortNone` configures `WebEnvironment.NONE` for tests that
  exercise the message/data-access/saga layers without spinning up the
  REST server (faster, no port contention).

Hand-rolling `@SpringBootTest` defeats both base classes — you lose the
framework's auto-config for test logging, `@MockBean` defaults, and
RestAssured plumbing, and your test diverges from the rest of the suite.

The `(test, local)` profile pair is the convention because lg5 services
typically bundle their default config under `local` (so `make run-apps`
just works in dev) and override with `test` when running ITs/ATDDs. Using
only `test` would lose the `local` defaults; using only `local` would lose
the test-specific overrides like `scheduling.enabled: false` (RULE-011).

## Example — correct

```java
// payment-container/src/test/java/.../OrderPaymentSagaIT.java
@ActiveProfiles({"test", "local"})
@Sql("/sql/payment-it-data.sql")
class OrderPaymentSagaIT extends Lg5TestBoot {

    @Autowired private PaymentRequestMessageListener listener;
    @Autowired private PaymentRepository             repository;

    @Test
    void should_complete_payment_saga_happy_path() {
        listener.completePayment(samplePaymentRequest());
        assertThat(repository.findById(SAMPLE_ID)).isPresent();
    }
}
```

```java
// payment-acceptance-test/src/test/java/.../CucumberSpringConfig.java
@ActiveProfiles({"test", "local"})
@CucumberContextConfiguration
public class CucumberSpringConfig extends Lg5TestBootPortNone { }
```

## Anti-pattern

```java
// WRONG: hand-rolled @SpringBootTest, missing one of the profiles
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentControllerIT { ... }                  // ❌ missing "local"

// WRONG: extending neither base class — loses RestAssured wiring and
//        every other framework default.
@SpringBootTest
@ActiveProfiles({"test", "local"})
class PaymentControllerIT { ... }                  // ❌ no Lg5TestBoot
```

## References

- Skill: `lg5-atdd` (full Cucumber + Testcontainers + Wiremock recipe).
- Framework classes: `com.lg5.spring.test.Lg5TestBoot`,
  `com.lg5.spring.test.Lg5TestBootPortNone`.
- Related rules: RULE-013 (Testcontainers gating), RULE-011 (test-time
  scheduling disabled via YAML).
