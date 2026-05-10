# Specs — `lg5-spring-agent-os`

This directory provides the **Spec-Driven Development (SDD)** scaffolding
for services that consume the bundle.

> SDD as practiced here follows the **spec-anchored** variant described
> by Birgitta Böckeler / Martin Fowler in
> [_Understanding Spec-Driven-Development_](https://martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html):
> specs live alongside the code, are kept in sync as the feature evolves,
> but the code remains directly editable (we are **not** spec-as-source).
>
> Workflow shape and vocabulary borrow heavily from
> [GitHub spec-kit](https://github.com/github/spec-kit).

## Layout

```
specs/
├── manifest.yaml
├── CHANGELOG.md
├── README.md                                  # this file
├── templates/
│   ├── prd-template.md                        # Specify
│   ├── adr-template.md                        # Plan
│   ├── plan-template.md                       # Plan
│   ├── tasks-template.md                      # Tasks
│   ├── data-model-template.md                 # Plan companion
│   └── research-template.md                   # optional spike
└── examples/
    └── loyalty-ledger/                        # an end-to-end example
        ├── prd.md, plan.md, tasks.md, data-model.md, README.md
        └── adr/ADR-001-*.md, ADR-002-*.md
```

## Workflow

```
   /sdd-specify     /sdd-plan         /sdd-tasks        /sdd-implement
       │                │                  │                  │
       ▼                ▼                  ▼                  ▼
     prd.md   ──►  plan.md + adr/  ──►  tasks.md   ──►   code + tests
                  + data-model.md       (TASK-NNN)        + commit
   (functional)   (technical)           (atomic)         (per task)
       │                │                  │                  │
       └─ HUMAN ────────┴────── HUMAN ─────┴── HUMAN ────────►
          APPROVES        APPROVES          APPROVES
```

Each phase produces one or more markdown files under
`docs/specs/<NNN-slug>/` in the **consumer** service repo.

## Per-feature folder shape (in the consumer repo)

```
docs/specs/<NNN-slug>/
├── prd.md           # produced by /sdd-specify
├── plan.md          # produced by /sdd-plan
├── data-model.md    # produced by /sdd-plan when there is persistent state
├── tasks.md         # produced by /sdd-tasks
├── research.md      # optional, spike result
└── adr/
    ├── ADR-001-<slug>.md
    └── ADR-002-<slug>.md
```

## Consumer service convention

- One folder per feature, numbered `001-`, `002-`, …
- All TASK-NNN for a feature commit on a branch named
  `feature/<NNN-slug>`; merged into `main` via PR when DoD is met.
- Each commit message references its TASK-ID:
  `feat(TASK-NNN): <title>`.

## Do NOT

- **Do NOT** write a 16-AC PRD for a 1-day feature. Size is proportional
  to the work.
- **Do NOT** mention technology in the PRD (no Spring, Kafka, Postgres,
  REST, Avro). That is what the Plan + ADRs are for.
- **Do NOT** skip the Definition-of-Done checklists at the bottom of each
  template. They are the gate the agent uses to decide a phase is done.

See [`examples/loyalty-ledger/`](examples/loyalty-ledger/) for an
end-to-end walkthrough.
