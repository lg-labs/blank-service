---
id: RULE-006
slug: rest-media-type
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: architecture
tags: [rest, media-type, versioning, content-negotiation]
description: REST controllers must produce `application/vnd.api.v1+json`. This is the conventional vendor media type used by all lg5-spring services for explicit API versioning.
---

# RULE-006 — REST media type

## Statement

Every `@RestController` in a consumer service must declare:

```java
produces = "application/vnd.api.v1+json"
```

either at class level (`@RequestMapping(produces = "application/vnd.api.v1+json")`)
or per endpoint (`@PostMapping(produces = "application/vnd.api.v1+json")`).
Never use the default `application/json`, never `text/plain`, never
`application/hal+json` unless you are explicitly building a HAL-compliant
hypermedia API.

When the API evolves to v2, change the media type to
`application/vnd.api.v2+json` and serve both versions in parallel during the
deprecation window.

## Rationale

Vendor media types (RFC 6838 `vnd.*` subtree) make API versioning **explicit
in the protocol**, not in the URL path. Clients negotiate a specific version
via the `Accept` header (`Accept: application/vnd.api.v1+json`) and the server
declares what it speaks via `Content-Type`. This means:

- URLs stay stable across versions (`/payments`, not `/v1/payments`).
- Different endpoints can evolve at different speeds (`payments` is v2 while
  `refunds` is still v1) without route gymnastics.
- The version is part of every request/response and shows up in logs, traces,
  and contract tests automatically.
- ATDD scenarios using RestAssured have a reliable `Accept` header to pin.

## Example — correct

```java
@RestController
@RequestMapping(value = "/payments", produces = "application/vnd.api.v1+json")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentApplicationService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public PaymentResponse create(@RequestBody @Valid final PaymentCommand cmd) {
        return service.handle(cmd);
    }
}
```

```gherkin
# ATDD step
When the client POSTs to /payments with header Accept: application/vnd.api.v1+json
Then the response Content-Type is application/vnd.api.v1+json
```

## Anti-pattern

```java
// WRONG: defaults to application/json — no version negotiation possible
@RestController
@RequestMapping("/payments")
public class PaymentController { ... }

// WRONG: version in the URL instead of the media type
@RequestMapping("/v1/payments")
public class PaymentControllerV1 { ... }

// WRONG: invented vendor type that doesn't follow the convention
@RequestMapping(value = "/payments", produces = "application/lg5+json")
```

## References

- Skill: `lg5-spring-overview` (REST conventions).
- RFC 6838 — Media Type Specifications and Registration Procedures.
- Related rules: RULE-005 (stock Spring annotations).
