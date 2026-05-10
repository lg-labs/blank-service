# Changelog — lg5-api-docs

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-10
### Added
- Initial skill capturing the static-HTML approach for OpenAPI and AsyncAPI
  documentation sites.
- `templates/openapi-template/index.html` — Swagger UI 5 wrapper loaded
  from `unpkg.com/swagger-ui-dist@5` (same renderer as
  petstore.swagger.io).
- `templates/asyncapi-template/index.html` — `@asyncapi/web-component@3`
  wrapper loaded from `unpkg.com` (same React component as
  studio.asyncapi.com).
- CI job snippets that assemble the site by `cp`-ing the template + YAML
  spec, replacing the legacy `asyncapi/cli` and
  `openapitools/openapi-generator-cli` Docker pipelines.
- Anti-patterns: Docker compose generators, NPM/puppeteer in CI, exact
  patch pinning of CDN URLs.
- Pinned against framework SHA `d0d754a`.
