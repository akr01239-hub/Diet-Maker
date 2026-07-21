# Architecture Decision Records

Lightweight ADRs. Newest first. Each: context, decision, consequences.

---

## ADR-0003 — Deterministic health logic lives on the server, never in an LLM

**Context:** BMR/TDEE/macros/safety limits must be correct and auditable. LLMs are
non-deterministic and can hallucinate numbers.

**Decision:** All health math is pure, unit-tested TypeScript in `server/src/calc` and
`server/src/guardrails`. The AI layer may only *propose* meal structures; nutrition is
always recomputed from the food DB and re-validated by guardrails before anything is saved.

**Consequences:** Calc + guardrails carry near-100% test coverage. The app is fully
functional with `AI_PROVIDER=rules` (no keys, no cost).

---

## ADR-0002 — Node + Express + TypeScript backend, deployed to Render

**Context:** Need a free-tier host and fast iteration. Spec allows Ktor as an alternative.

**Decision:** Node + Express + TS (layered: routes → controllers → services →
repositories), Prisma ORM to Neon Postgres, deployed as a Render free web service via
`render.yaml`. Redis is optional (in-memory fallback).

**Consequences:** Render free tier sleeps after ~15 min idle (cold start ~30–60s). Documented
in the README; an optional uptime pinger can keep it warm. Single language on the server;
Android is a separate Kotlin client.

---

## ADR-0001 — Native Android (Kotlin + Compose), offline-first client

**Context:** Premium native app; the server is the source of truth for calculations, plans,
and safety.

**Decision:** Kotlin + Jetpack Compose + Material 3, Clean Architecture + MVVM, Hilt,
Coroutines/Flow, Retrofit + Room + DataStore, WorkManager. The app caches today's plan and
logs in Room and syncs via WorkManager.

**Consequences:** Works offline for core flows; the server owns all authoritative
computation. Min SDK 26.
