---
description: Scaffold the canonical lg5-spring CI/CD pipeline (GitHub Actions workflow, Maven-credentials composite action, Swagger UI / AsyncAPI doc templates, Allure Report wiring) into a consumer service repo by copying the lg5-github-actions, lg5-api-docs and lg5-allure-report skill templates and rewriting `<svc>` placeholders.
argument-hint: <service-name>
allowed-tools: bash, read, write, edit, glob, grep
---

# /scaffold-ci-cd

You are installing the canonical lg5-spring CI/CD pipeline into a
consumer microservice repository. This command consumes the templates
shipped by three skills:

- `lg5-github-actions` — GitHub Actions workflow + Maven creds action.
- `lg5-api-docs` — Swagger UI / AsyncAPI Studio HTML wrappers.
- `lg5-allure-report` — Allure properties (the Maven dep + Cucumber
  plugin registration are added as edits, not template files).

## Inputs

- `<service-name>` — the consumer service name in lowercase-hyphen form
  (e.g. `payment`, `loyalty-ledger`). Drives the `<svc>-…` module names
  in the workflow and template paths.

If the user did not provide it, ask for it BEFORE making any file
changes.

## Pre-flight checks

1. Verify the consumer repo is at the root of the working directory
   (look for `pom.xml` and an `<svc>-acceptance-test/` module).
2. Verify the bundle is installed at `.agent-os/skills/lg5-github-actions/`,
   `.agent-os/skills/lg5-api-docs/`, `.agent-os/skills/lg5-allure-report/`.
   If not, ask the user to run `bin/install.sh` from the bundle first.
3. Check whether `.github/workflows/c-integration.yml`,
   `.github/actions/setup-maven-credentials/action.yml`,
   `<svc>-support/openapi-template/index.html`,
   `<svc>-support/asyncapi-template/index.html`,
   or `<svc>-acceptance-test/src/test/resources/allure.properties`
   already exist. If yes, ask the user before overwriting.

## Steps

### 1) Composite action (`lg5-github-actions`)

Copy without modification:

```bash
mkdir -p .github/actions/setup-maven-credentials
cp .agent-os/skills/lg5-github-actions/templates/.github/actions/setup-maven-credentials/action.yml \
   .github/actions/setup-maven-credentials/action.yml
```

### 2) Workflow (`lg5-github-actions`)

Copy and replace the `blank-` prefixes with `<service-name>-`:

```bash
mkdir -p .github/workflows
cp .agent-os/skills/lg5-github-actions/templates/.github/workflows/c-integration.yml \
   .github/workflows/c-integration.yml

# Replace module path prefixes (the workflow ships with blank-* paths)
sed -i.bak \
  -e "s/blank-support/<service-name>-support/g" \
  -e "s/blank-api/<service-name>-api/g" \
  -e "s/blank-message/<service-name>-message/g" \
  -e "s/blank-acceptance-test/<service-name>-acceptance-test/g" \
  -e "s/blank-service-image/<service-name>-service-image/g" \
  .github/workflows/c-integration.yml
rm -f .github/workflows/c-integration.yml.bak
```

Tell the user to add **`PKG_GITHUB_TOKEN`** as a repo secret with
`read:packages` on `lg-labs-pentagon`.

### 3) API doc templates (`lg5-api-docs`)

```bash
mkdir -p <service-name>-support/openapi-template <service-name>-support/asyncapi-template
cp .agent-os/skills/lg5-api-docs/templates/openapi-template/index.html \
   <service-name>-support/openapi-template/index.html
cp .agent-os/skills/lg5-api-docs/templates/asyncapi-template/index.html \
   <service-name>-support/asyncapi-template/index.html
```

Both files are spec-agnostic — no rewriting needed.

### 4) Allure properties (`lg5-allure-report`)

```bash
mkdir -p <service-name>-acceptance-test/src/test/resources
cp .agent-os/skills/lg5-allure-report/templates/src/test/resources/allure.properties \
   <service-name>-acceptance-test/src/test/resources/allure.properties
```

### 5) Allure Maven deps (`lg5-allure-report`) — edit, don't copy

Add to `<service-name>-acceptance-test/pom.xml` inside `<dependencies>`:

```xml
<!-- Allure Report (Cucumber 7 + JUnit Platform) -->
<dependency>
  <groupId>io.qameta.allure</groupId>
  <artifactId>allure-cucumber7-jvm</artifactId>
  <version>2.29.1</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.qameta.allure</groupId>
  <artifactId>allure-junit-platform</artifactId>
  <version>2.29.1</version>
  <scope>test</scope>
</dependency>
```

### 6) Allure Cucumber plugin registration (`lg5-allure-report`) — edit

In `<service-name>-acceptance-test/src/test/java/.../AcceptanceTestCase.java`,
append the Allure plugin to the `PLUGIN_PROPERTY_NAME` value:

```java
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty, " +
    "json:target/atdd-reports/cucumber.json, " +
    "html:target/atdd-reports/cucumber-reports.html, " +
    "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"),
```

If the file already has the legacy three-plugin string, only **add** the
trailing `", io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"`.

### 7) Logback portability (advisory — only if needed)

If the consumer's `<svc>-container/src/main/resources/config/application.yaml`
hard-codes a `log.path`, replace it with:

```yaml
log:
  path: ${APPLICATION_LOG_DESTINATION_PATH:${java.io.tmpdir}}
```

This unblocks the `test` job, which sets `APPLICATION_LOG_DESTINATION_PATH:
"./target/logs"`.

## Verification

After scaffolding, instruct the user to:

1. Add the `PKG_GITHUB_TOKEN` secret in the repo settings.
2. Commit the changes on a feature branch.
3. Push and open a PR. The 11-job CI pipeline should run end-to-end.
4. Inspect the artifacts:
   - `docker-image` (from `build`)
   - `cucumber-report` and `allure-results` (from `test`)
   - `openapi-doc`, `asyncapi-doc`, `allure-report` (downstream jobs)
5. For local preview: download the doc artifact and run
   `python3 -m http.server 8765` from the directory.

## Anti-patterns (refuse if requested)

- ❌ Asking to skip the composite action and inline `~/.m2/settings.xml`
  in each job. Refuse and reference `lg5-github-actions`.
- ❌ Asking to remove the legacy Cucumber `pretty/json/html` plugins
  when adding Allure. Refuse — Allure is *additive*.
- ❌ Asking to add `allure-maven-plugin` to the build lifecycle. Refuse
  — we use the JVM plugin + CLI to keep Maven decoupled from the UI.
- ❌ Asking to pin the Allure CLI to `latest`. Pin the version
  (currently `2.32.0`).

## See also

- `lg5-github-actions/SKILL.md` — full pipeline topology and conventions.
- `lg5-api-docs/SKILL.md` — Swagger UI / AsyncAPI rendering pattern.
- `lg5-allure-report/SKILL.md` — Allure wiring details.
- `/scaffold-service` — scaffolds the *whole* service from
  `blank-service`. Run that first if starting from scratch; this command
  is for already-existing services that need the CI/CD layer.
