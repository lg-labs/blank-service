---
name: lg5-api-docs
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-10
description: How to publish browseable HTML documentation for the REST (OpenAPI) and event (AsyncAPI) contracts of an lg5-spring service. Uses static HTML wrappers that load Swagger UI 5 and the AsyncAPI web-component from unpkg CDN — no build step, no Docker, same renderers as petstore.swagger.io and studio.asyncapi.com. Replaces the legacy `asyncapi/cli` + `openapitools/openapi-generator-cli` Docker pipelines that broke on `--use-new-generator` and puppeteer install. Load this skill when the user asks about API docs, OpenAPI HTML, AsyncAPI HTML, Swagger UI, AsyncAPI Studio look-alike, or how to publish the contract sites from CI.
---

# lg5-spring — API Documentation Sites (OpenAPI + AsyncAPI)

> Reference impl:
> - `blank-service/blank-support/openapi-template/index.html`
> - `blank-service/blank-support/asyncapi-template/index.html`
> - The `openapi` and `asyncapi` CI jobs in
>   `blank-service/.github/workflows/c-integration.yml`.

## Why this exists

The previous lg5-spring-style services rendered API docs by:
- **OpenAPI** — running `openapitools/openapi-generator-cli` in a Docker
  container, which forced a heavy compose file (`spec-generator.yml`) and
  produced the dated "static-html" template.
- **AsyncAPI** — running `asyncapi/cli` with the html-template Docker
  image. This broke on the `--use-new-generator` flag and on puppeteer
  installation in CI.

Both pipelines were brittle and slow. This skill replaces them with
**two ~70-line static HTML wrappers** that load the official renderers
from CDN. The CI job now just copies the template alongside the YAML
spec and uploads the directory as an artifact.

## Pattern (identical for OpenAPI and AsyncAPI)

1. Keep a tiny `index.html` in `<svc>-support/<openapi|asyncapi>-template/`.
   It loads the official renderer from `unpkg.com` and `fetch`-es a
   sibling `<openapi|asyncapi>.yaml` at runtime.
2. The CI job assembles the site by copying:
   - the template `index.html` →  `<svc>-support/<openapi|asyncapi>/index.html`
   - the spec YAML (from the API or message-model module) →  `<svc>-support/<openapi|asyncapi>/<spec>.yaml`
3. Upload the directory as a workflow artifact (or push to GH Pages).

No Maven plugin, no Docker, no NPM install.

## OpenAPI template (Swagger UI 5)

Renderer: `swagger-ui-dist@5` from unpkg — same renderer used by
[petstore.swagger.io](https://petstore.swagger.io/).

Key bits:

```html
<link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
<script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js" crossorigin></script>
<script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js" crossorigin></script>
<script>
  window.onload = () => {
    window.ui = SwaggerUIBundle({
      url: "./openapi.yaml",
      dom_id: "#swagger-ui",
      deepLinking: true,
      presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
      plugins: [SwaggerUIBundle.plugins.DownloadUrl],
      layout: "StandaloneLayout",
    });
  };
</script>
```

Full file: `templates/openapi-template/index.html` (71 lines).

CI job (excerpt):

```yaml
openapi:
  name: OpenAPI
  runs-on: ubuntu-latest
  needs: test
  steps:
    - uses: actions/checkout@v5
    - name: Assemble OpenAPI site (Swagger UI via CDN)
      run: |
        set -euo pipefail
        mkdir -p ./<svc>-support/openapi
        cp ./<svc>-support/openapi-template/index.html ./<svc>-support/openapi/index.html
        cp ./<svc>-api/src/main/resources/spec/openapi.yaml ./<svc>-support/openapi/openapi.yaml
    - uses: actions/upload-artifact@v4
      with:
        name: openapi-doc
        path: ./<svc>-support/openapi
```

## AsyncAPI template (Studio look)

Renderer: `@asyncapi/web-component@3` + `@asyncapi/react-component@2`
styles, both from unpkg — same React component used by
[studio.asyncapi.com](https://studio.asyncapi.com/).

Key bits:

```html
<link rel="stylesheet" href="https://unpkg.com/@asyncapi/react-component@2/styles/default.min.css" />
<script src="https://unpkg.com/@asyncapi/web-component@3/lib/asyncapi-web-component.js" defer></script>
<script>
  (async () => {
    const container = document.getElementById("asyncapi-container");
    const res = await fetch("./asyncapi.yaml", { cache: "no-cache" });
    const schema = await res.text();
    const component = document.createElement("asyncapi-component");
    component.setAttribute("cssImportPath",
      "https://unpkg.com/@asyncapi/react-component@2/styles/default.min.css");
    component.schema = schema;
    container.innerHTML = "";
    container.appendChild(component);
  })();
</script>
```

Full file: `templates/asyncapi-template/index.html` (69 lines).

CI job (excerpt):

```yaml
asyncapi:
  name: AsyncAPI
  runs-on: ubuntu-latest
  needs: test
  steps:
    - uses: actions/checkout@v5
    - name: Assemble AsyncAPI site (Studio-like, web-component via CDN)
      run: |
        set -euo pipefail
        mkdir -p ./<svc>-support/asyncapi
        cp ./<svc>-support/asyncapi-template/index.html ./<svc>-support/asyncapi/index.html
        cp ./<svc>-message/<svc>-message-model/src/main/resources/spec/asyncapi.yaml \
           ./<svc>-support/asyncapi/asyncapi.yaml
    - uses: actions/upload-artifact@v4
      with:
        name: asyncapi-doc
        path: ./<svc>-support/asyncapi
```

## Where the spec YAMLs live

| Spec        | Canonical location in an lg5-spring service                                  |
|-------------|------------------------------------------------------------------------------|
| OpenAPI     | `<svc>-api/src/main/resources/spec/openapi.yaml`                             |
| AsyncAPI    | `<svc>-message/<svc>-message-model/src/main/resources/spec/asyncapi.yaml`    |

The AsyncAPI YAML lives in the **message-model** module because that's
where Avro schemas (RULE-007) and event contracts already live — keeping
spec and code colocated.

## Conventions

- **Pin to majors** (`@5`, `@3`, `@2`) on unpkg URLs to balance "always
  fresh" with reproducibility. Don't pin to exact patches; you'd be
  re-bumping monthly.
- **No NPM install** in the consumer repo. The browser fetches the
  bundle at view time.
- **Local preview** is one-line: `cd <svc>-support/openapi && python3 -m
  http.server 8765`.
- **GH Pages**: point Pages at the directory uploaded as `openapi-doc`
  / `asyncapi-doc` artifacts (or merge them into the `docs` job site).

## Anti-patterns

- ❌ Running `openapitools/openapi-generator-cli` in a Docker compose
  file (`spec-generator.yml`). Slow, opaque, dated UI.
- ❌ Running `asyncapi/cli` with the html-template Docker image. Breaks
  on `--use-new-generator` and on puppeteer install.
- ❌ Pinning the CDN URL to an exact patch (`@5.10.3`). Forces monthly
  bumps. Pin majors instead.
- ❌ Inlining the YAML spec into the HTML at build time. Defeats the
  purpose of static templates and hurts diffability.

## Local & CI parity

The exact same `index.html` works:
- Locally via `python3 -m http.server` (or any static file server).
- In CI, after the `Assemble … site` step (it's just `cp` + spec).
- On GH Pages or any static host (S3, Netlify, etc.).

## See also

- `lg5-github-actions` — the workflow that hosts the `openapi` /
  `asyncapi` jobs.
- `/scaffold-ci-cd` command — copies these templates and the matching
  CI jobs in one shot.
