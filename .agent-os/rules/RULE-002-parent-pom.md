---
id: RULE-002
slug: parent-pom
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: framework
tags: [maven, parent-pom, framework-pin, sha]
description: Every consumer service's pom.xml must inherit from `com.lg5.spring:lg5-spring-parent:1.0.0-alpha.<short-git-sha>` where the SHA is the framework commit being consumed.
---

# RULE-002 — Parent POM

## Statement

Every consumer microservice (root `pom.xml` of the multi-module project) must
inherit from:

```xml
<parent>
  <groupId>com.lg5.spring</groupId>
  <artifactId>lg5-spring-parent</artifactId>
  <version>1.0.0-alpha.<short-git-sha-of-framework></version>
</parent>
```

The version suffix is the **short (7-char) git SHA** of the
[`lg5-spring`](https://github.com/lg-labs-pentagon/lg5-spring) commit that the
service is consuming. Never invent a version string and never use `LATEST`,
`SNAPSHOT`, `1.0.0`, or any other free-form version.

## Rationale

The lg5-spring framework does **not** publish to Maven Central. It publishes
artifacts to the local Maven repository (and internal registries) under the
synthetic version `1.0.0-alpha.<sha>`. The SHA in the version is the
single mechanism that ties a deployed service to a reproducible framework
revision — it is what the bundle's `lg5-spring-sha` field tracks and what the
agent-os validator enforces across all artifacts.

A wrong or missing SHA means the build either fails to resolve the parent or,
worse, picks up an unintended local snapshot, leading to silent drift between
environments.

## Example — correct

```xml
<parent>
  <groupId>com.lg5.spring</groupId>
  <artifactId>lg5-spring-parent</artifactId>
  <version>1.0.0-alpha.d0d754a</version>
</parent>
```

To advance to a newer framework commit:

1. Pull the latest `lg5-spring`, run `make publish-local`.
2. Note the short SHA of the new HEAD (`git rev-parse --short=7 HEAD`).
3. Bump the `<version>` in this rule and in every consumer service's parent.
4. Bump the agent-os bundle `lg5-spring-sha` in `skills/manifest.yaml` and
   `rules/manifest.yaml` and re-validate every artifact.

## Anti-pattern

```xml
<!-- WRONG: invented or unpinned version -->
<version>1.0.0-alpha</version>
<version>LATEST</version>
<version>1.0.0-SNAPSHOT</version>
```

Also wrong: omitting the parent and pulling individual `lg5-spring-*`
dependencies à la carte. The parent is the contract; bypassing it breaks
dependency convergence and disables auto-configuration provided by the
framework starter.

## References

- Skill: `lg5-new-service` (full project setup walkthrough).
- Skill: `lg5-spring-overview` (framework versioning convention).
- Related rules: RULE-001 (stack baseline), RULE-017 (build commands).
