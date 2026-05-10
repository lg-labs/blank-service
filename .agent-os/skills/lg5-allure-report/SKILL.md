---
name: lg5-allure-report
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-10
description: How to wire Allure Report into the Cucumber acceptance-test module of an lg5-spring service. Covers `allure-cucumber7-jvm` + `allure-junit-platform` 2.29.1 dependencies, registering `io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm` as a Cucumber plugin in `AcceptanceTestCase`, pinning the results directory via `allure.properties`, and the CI job that downloads Allure CLI 2.32.0 and generates the HTML dashboard. Load this skill when the user asks about Allure, Cucumber HTML reports, test dashboards, replacing the default Cucumber HTML output, or adding Allure to an `<svc>-acceptance-test` module.
---

# lg5-spring — Allure Report (Cucumber + JUnit Platform)

> Reference impl: `blank-service/blank-acceptance-test/` and the `allure`
> CI job in `blank-service/.github/workflows/c-integration.yml`.

## Why this exists

The default Cucumber `pretty/json/html` plugins produce a single
`cucumber-reports.html` that's enough for CI sanity but not for browsing
trends, attaching screenshots, grouping by epic/feature/story, or
diff-ing runs. Allure Report fixes all of that with a polished,
filterable dashboard — and it plugs into Cucumber 7 + JUnit Platform
without forcing a Maven plugin on the build.

## Wiring in three places

### 1) `<svc>-acceptance-test/pom.xml` — add two deps

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

> Both pinned to `2.29.1` (latest stable as of 2026-05-10; aligned with
> the Allure CLI 2.32.0 used in CI).

### 2) `AcceptanceTestCase.java` — register the Cucumber plugin

Append `io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm` to the existing
`PLUGIN_PROPERTY_NAME` value. Keep the legacy plugins so backward
consumers (CI badges, IDE) still work:

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
    @ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty, " +
        "json:target/atdd-reports/cucumber.json, " +
        "html:target/atdd-reports/cucumber-reports.html, " +
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"),
    @ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.<org>.<svc>")
})
class AcceptanceTestCase { /* … */ }
```

### 3) `src/test/resources/allure.properties` — pin the results dir

Single line. Required so the Allure CLI in CI knows where to read from:

```properties
allure.results.directory=target/allure-results
```

> Full file: `templates/src/test/resources/allure.properties`.

## CI job (Allure CLI 2.32.0)

Lives downstream of `test` (the ATDD job) in `c-integration.yml`. Runs
even when `test` fails (`if: always()`) so flaky-run dashboards are
preserved:

```yaml
allure:
  name: Allure Report
  runs-on: ubuntu-latest
  needs: test
  if: always()
  steps:
    - uses: actions/checkout@v5

    - name: Download Allure raw results
      uses: actions/download-artifact@v5
      with:
        name: allure-results
        path: ./allure-results
      continue-on-error: true

    - name: Set up Allure CLI
      run: |
        set -euo pipefail
        ALLURE_VERSION=2.32.0
        curl -sL "https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.tgz" \
          | tar -xz -C "$HOME"
        echo "$HOME/allure-${ALLURE_VERSION}/bin" >> "$GITHUB_PATH"

    - name: Generate Allure HTML report
      run: |
        set -euo pipefail
        allure --version
        allure generate ./allure-results --clean -o ./allure-report

    - name: Upload Allure HTML report
      uses: actions/upload-artifact@v4
      with:
        name: allure-report
        path: ./allure-report
```

The `test` job must upload `allure-results` for this to consume:

```yaml
- name: Upload Allure raw results
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: allure-results
    path: ./<svc>-acceptance-test/target/allure-results
    if-no-files-found: ignore
```

## Conventions

- **Don't add the `allure-maven-plugin`**. We rely on the JVM Cucumber
  plugin to *write* results and the Allure CLI to *render* them. Keeps
  the Maven build lean and the CLI version (which controls the UI)
  decoupled from the deps.
- **Keep the legacy `pretty/json/html` Cucumber plugins**. The Allure
  plugin is *additive*, not a replacement.
- **Pin the CLI** (`ALLURE_VERSION=2.32.0`) for reproducibility across
  runs. Bump deliberately.
- **`if: always()` + `continue-on-error`** on the download step ensures
  the dashboard still renders when ATDD fails — that's exactly when you
  need it most.

## Anti-patterns

- ❌ Replacing `html:target/atdd-reports/cucumber-reports.html` with the
  Allure plugin alone. Some downstream consumers (PR comment bots, CI
  badges) still need the plain Cucumber HTML.
- ❌ Adding `io.qameta.allure:allure-maven` as a `<plugin>` in the build
  lifecycle. Couples Maven to the Allure UI version.
- ❌ Letting the CLI download float on `latest`. Reproducibility broken
  on every Allure release.
- ❌ Hard-coding the results path in Java code (e.g.
  `Allure.setResultsDirectory(...)`). Use `allure.properties`.

## Verifying locally

```bash
# Run ATDD as usual
make run-atdd-module

# Render the dashboard
brew install allure   # or download CLI 2.32.0 manually
allure serve <svc>-acceptance-test/target/allure-results
```

A browser opens with the dashboard. `allure generate --clean -o
./allure-report` produces a static directory you can host anywhere.

## See also

- `lg5-atdd` — the Cucumber + Testcontainers wiring this skill plugs
  into.
- `lg5-github-actions` — the workflow that hosts the `allure` job.
- `/scaffold-ci-cd` command — copies all the wiring at once.
