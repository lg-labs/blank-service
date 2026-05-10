---
id: RULE-001
slug: stack-baseline
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: framework
tags: [versions, spring-boot, jdk, kotlin, build-tool]
description: Pin the stack baseline (Spring Boot 3.4.2, Spring 6.2.2, JDK 21, Kotlin 21, Gradle for framework / Maven for services). Never propose lower versions.
---

# RULE-001 — Stack baseline

## Statement

Every project — framework module or consumer microservice — must use exactly:

| Component        | Version  |
|------------------|----------|
| Spring Boot      | **3.4.2** |
| Spring Framework | **6.2.2** |
| JDK              | **21**   |
| Kotlin           | **21**   |
| Build tool       | **Gradle** (framework) / **Maven** (services) |

Never propose lower versions, never mix build tools inside a single module, and
never silently upgrade to newer Spring Boot/Spring Framework patches without
re-validating the whole bundle (the framework declares these versions in its
`gradle/libs.versions.toml`).

## Rationale

The lg5-spring framework is built and tested as a unit against this exact
matrix. Down-pinning Spring Boot loses features the framework relies on
(e.g. virtual threads, bind handlers); up-pinning risks breaking the binary
contract of `lg5-spring-parent` which is the single source of truth. JDK 21 is
required because the framework uses pattern matching, records, and virtual
threads. Mixing Gradle and Maven inside a service breaks the `make` targets
that drive the local dev loop.

## Example — correct

`pom.xml` of a consumer service:

```xml
<parent>
  <groupId>com.lg5.spring</groupId>
  <artifactId>lg5-spring-parent</artifactId>
  <version>1.0.0-alpha.d0d754a</version>
</parent>

<properties>
  <java.version>21</java.version>
</properties>
```

The Spring Boot version is **inherited** from the parent — never restated.

## Anti-pattern

```xml
<!-- WRONG: overrides the parent's pinned versions -->
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.0</version>
</parent>

<properties>
  <java.version>17</java.version>
</properties>
```

This bypasses `lg5-spring-parent` entirely, breaks dependency convergence with
the rest of the framework's BOM, and removes the JDK 21 features the framework
uses internally.

## References

- Skill: `lg5-spring-overview` (full module map and version policy).
- Framework version catalog: `lg5-spring/gradle/libs.versions.toml`.
- Related rules: RULE-002 (parent), RULE-005 (no custom annotations).
