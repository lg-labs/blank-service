---
name: lg5-github-actions
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-10
description: How to wire a GitHub Actions Continuous Integration pipeline for an lg5-spring service. Covers the canonical 11-job topology (Setup → Visualization, Checkstyle, Coverage → Quality, Build → Acceptance Test → OpenAPI, AsyncAPI, Allure Report, Docs), the shared `setup-maven-credentials` composite action that provisions `~/.m2/settings.xml` with the GitHub Packages registry of the `lg-labs-pentagon` org, Maven cache restore, JDK 21 (zulu), Make-targets-driven steps, and Docker image hand-off between `build` and `test`. Load this skill when the user asks about CI/CD, GitHub Actions, Maven authentication failures, the `PKG_GITHUB_TOKEN` secret, the composite action, the Continuous Integration workflow, or how to ship the `c-integration.yml` pipeline to a new service.
---

# lg5-spring — GitHub Actions CI Pipeline

> Reference impl: `blank-service/.github/workflows/c-integration.yml` (the
> canonical CI pipeline that every lg5-spring service mirrors).
> Reference impl: `blank-service/.github/actions/setup-maven-credentials/action.yml`
> (the shared composite action solved the Maven 401 issue across parallel jobs).

## Why this exists

The lg5-spring parent POM is published to **GitHub Packages under the
`lg-labs-pentagon` organization**. Resolving it requires authenticated
Maven access on every job that touches `mvn`. Without the composite
action, each job had to inline its own `~/.m2/settings.xml`, which led
to silent skews, copy-paste drift, and 401 failures in parallel jobs
(checkstyle, coverage, build, test) that were hard to debug.

This skill captures the **single shared composite action** plus the
**11-job topology** that proves the action works end-to-end (build,
test with Docker hand-off, docs, reports).

## The composite action (one source of truth)

Path in consumer repo: `.github/actions/setup-maven-credentials/action.yml`.

```yaml
name: 'Setup Maven credentials'
description: |
  Generates ~/.m2/settings.xml with credentials for the GitHub Packages
  Maven registry of the lg-labs-pentagon org. Required by every job that
  runs `mvn` and depends on the lg5-spring parent POM.

inputs:
  github-username:
    description: 'GitHub username for the package registry'
    required: false
    default: ${{ github.actor }}
  github-token:
    description: 'GitHub token with read:packages on lg-labs-pentagon'
    required: true

runs:
  using: 'composite'
  steps:
    - name: Configure Maven settings.xml
      shell: bash
      env:
        GITHUB_USERNAME: ${{ inputs.github-username }}
        GITHUB_TOKEN: ${{ inputs.github-token }}
      run: |
        mkdir -p ~/.m2
        cat > ~/.m2/settings.xml <<EOF
        <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" ...>
          <servers>
            <server>
              <id>github</id>
              <username>${GITHUB_USERNAME}</username>
              <password>${GITHUB_TOKEN}</password>
            </server>
          </servers>
          <profiles>
            <profile>
              <id>github</id>
              <repositories>
                <repository>
                  <id>github</id>
                  <url>https://maven.pkg.github.com/lg-labs-pentagon/*</url>
                  <snapshots><enabled>true</enabled></snapshots>
                </repository>
              </repositories>
            </profile>
          </profiles>
          <activeProfiles>
            <activeProfile>github</activeProfile>
          </activeProfiles>
        </settings>
        EOF
```

> See `templates/.github/actions/setup-maven-credentials/action.yml` for
> the byte-identical copy-paste version.

### Required secret on the consumer repo

| Secret name          | Scope                                           |
|----------------------|-------------------------------------------------|
| `PKG_GITHUB_TOKEN`   | Classic PAT or fine-grained PAT with `read:packages` on `lg-labs-pentagon`. Used by every job that runs `mvn`. |

## Pipeline topology (11 jobs)

```
                       ┌──────────────┐
                       │    setup     │  (extract Maven coords, cache, creds)
                       └──┬─────┬─────┘
            ┌─────────────┘     │   └─────────────┐
            ▼                   ▼                 ▼
     ┌────────────┐      ┌────────────┐    ┌──────────────┐
     │ checkstyle │      │  coverage  │    │ visualization │
     └─────┬──────┘      └─────┬──────┘    └──────────────┘
           │                   │
           └────────┬──────────┘
                    ▼
           ┌──────────────┐    ┌─────────┐
           │   quality    │    │  build  │  (jib → docker save → artifact)
           └──────────────┘    └────┬────┘
                                    ▼
                              ┌──────────┐
                              │   test   │  (load image, run ATDD, upload)
                              └────┬─────┘
                  ┌──────────┬─────┴──────┬──────────┐
                  ▼          ▼            ▼          ▼
              ┌────────┐ ┌─────────┐ ┌────────┐ ┌────────┐
              │openapi │ │asyncapi │ │ allure │ │  docs  │
              └────────┘ └─────────┘ └────────┘ └────────┘
```

| Job             | Depends on            | What it does                                                       |
|-----------------|-----------------------|--------------------------------------------------------------------|
| `setup`         | —                     | Extracts `groupId/artifactId/version/IMAGE_NAME` from `pom.xml` via `xmllint`, primes Maven cache, runs the composite action. Uploads `maven-details` artifact. |
| `visualization` | `setup`               | Runs `NBprojekt/gource-action@v1.2.1` and uploads the `gource.mp4`. |
| `checkstyle`    | `setup`               | `make run-checkstyle`.                                             |
| `coverage`      | `setup`               | `make run-verify`; uploads `test-reports` (jacoco-aggregate-all).  |
| `quality`       | `checkstyle, coverage`| Codacy/Sonar reporter (placeholder).                               |
| `build`         | `checkstyle, coverage`| `make install-skip-test` → `docker save` → uploads `docker-image`, `dependency-graph`, `firebase-json` artifacts. |
| `test`          | `build`               | Loads the docker image, runs `make run-atdd-module`, uploads `cucumber-report` and `allure-results`. |
| `openapi`       | `test`                | Assembles Swagger UI site from `openapi.yaml` + the API-docs template. Uploads `openapi-doc`. (See `lg5-api-docs` skill.) |
| `asyncapi`      | `test`                | Assembles AsyncAPI Studio-like site from `asyncapi.yaml` + the API-docs template. Uploads `asyncapi-doc`. (See `lg5-api-docs` skill.) |
| `allure`        | `test` (`if: always`) | Downloads Allure CLI 2.32.0, runs `allure generate`, uploads `allure-report`. (See `lg5-allure-report` skill.) |
| `docs`          | `test, visualization` | mkdocs-material build, uploads `mkdocs` artifact.                  |

> The full byte-for-byte workflow lives at
> `templates/.github/workflows/c-integration.yml`.

## Conventions

- **JDK 21 (zulu)** on every job, matching RULE-001.
- **Cache key** is `${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}`
  with restore key `${{ runner.os }}-m2`. Keep both stable.
- **Make targets are mandatory** (RULE-017): `make run-checkstyle`,
  `make run-verify`, `make install-skip-test`, `make run-atdd-module`.
  Inline `mvn` invocations are an anti-pattern.
- **Docker image hand-off**: `build` saves the SUT image as `tar`,
  `test` downloads + `docker load`s it. The image is then started by the
  ATDD's testcontainer (`AppContainerCustomConfig`, see `lg5-atdd`).
- **Acceptance-test env vars** that must be set on the `test` job:
  ```yaml
  APPLICATION_TRACES_FILE_ENABLED: "false"
  APPLICATION_LOG_ENABLED: "false"
  APPLICATION_LOG_SOURCE_PATH: "/logs"
  APPLICATION_LOG_DESTINATION_PATH: "./target/logs"
  ```
  Without these, the SUT container spams logs into the runner's
  filesystem and may crash on read-only mounts.

## Anti-patterns

- ❌ Inlining `~/.m2/settings.xml` per job. Use the composite action.
- ❌ Hard-coding `${{ secrets.GITHUB_TOKEN }}` for the package registry —
  the default token can't read packages from another org. Use a PAT
  named `PKG_GITHUB_TOKEN`.
- ❌ Calling `mvn` directly in workflow steps. Use `make` targets.
- ❌ Mixing `actions/checkout@v4` and `@v5` — keep one major across
  jobs (this skill standardizes on `@v5`).
- ❌ Forgetting `fetch-depth: 0` on `setup` and `visualization` —
  `gource` needs the full history.

## Variations

- **Single-module services**: skip `coverage` (no `jacoco-aggregate-all`)
  and replace with a plain `mvn verify`.
- **No Docker image**: skip the `docker save / load` hand-off and run
  ATDD directly via Spring Boot Test (still needs the composite action
  for Maven creds).
- **Self-hosted runners**: keep the composite action; only change
  `runs-on`.

## See also

- `lg5-api-docs` — Swagger UI / AsyncAPI web-component templates that
  the `openapi` and `asyncapi` jobs deploy.
- `lg5-allure-report` — Allure wiring that the `allure` job consumes.
- `lg5-atdd` — Cucumber + testcontainers wiring that the `test` job
  exercises.
- `/scaffold-ci-cd` command — copies all the above templates into a
  consumer repo in one shot.
