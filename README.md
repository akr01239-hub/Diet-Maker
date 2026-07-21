# NutriAI (Diet-Maker)

A premium **native Android** diet & nutrition app backed by a small **Node API**.
100% free-tier: Neon (Postgres), Render (API hosting), USDA FoodData Central, Open Food
Facts, Firebase Cloud Messaging, Google OAuth. **No paid API, key, or subscription.**

> **Educational guidance, not medical advice.** Always consult a professional. Safety
> guardrails are central to every feature — see [`SECURITY.md`](SECURITY.md) and the
> guardrails module.

## Monorepo layout

```
Diet-Maker/
  android/     # Kotlin + Jetpack Compose client (offline-first)
  server/      # Node + Express + TypeScript API (deploy target: Render)
  render.yaml  # Render blueprint (infra-as-code)
  docs/        # ADRs and notes
```

The **server is the single source of truth** for calculations, plans, and safety. The
Android app is an offline-first client.

## Architecture at a glance

- **Server:** Express + TypeScript, layered (routes → controllers → services →
  repositories), Prisma ORM → Neon Postgres. Auth via JWT + Google OAuth + email OTP.
  Pluggable `AiProvider`, default `rules` (deterministic, no key). Redis optional
  (in-memory fallback).
- **Android:** Kotlin + Compose + Material 3, Clean Architecture + MVVM, Hilt, Coroutines /
  Flow, Retrofit + Room + DataStore, WorkManager, CameraX + ML Kit, Health Connect, FCM.
  Min SDK 26.

## Zero-cost / no-paid-API rules

The whole app must work fully with `AI_PROVIDER=rules` — a deterministic meal-plan
generator and food-DB/templated chat. `ollama` / `gemini` / `groq` are optional free
upgrades; their numeric output is always recomputed and re-validated by guardrails.

## Getting started (server, local)

Requires Node 20+.

```bash
cd server
cp .env.example .env          # then fill in Neon URLs and secrets
npm install
npx prisma generate
npx prisma migrate deploy     # applies migrations to Neon (uses DIRECT_URL)
npm run dev                   # http://localhost:8080
```

Health checks:

- `GET /health`  → liveness (always 200 when the process is up)
- `GET /ready`   → readiness (verifies DB connectivity)

## Android

Open `android/` in Android Studio (JDK 17). Create `android/local.properties` (git-ignored):

```
API_BASE_URL=https://<your-render-service>.onrender.com/api/v1
```

For the emulator against a local server use `http://10.0.2.2:8080/api/v1`.

## Deploy (Render, free web service)

1. Push to `main`.
2. In Render, create a **Blueprint** from this repo (it reads `render.yaml`).
3. In the Render dashboard set the `sync: false` secrets: `DATABASE_URL`, `DIRECT_URL`
   (from Neon), `HEALTH_DATA_ENCRYPTION_KEY`, `USDA_FDC_API_KEY`.
4. Auto-deploys on every push to `main`.

**Free-tier caveat:** the service sleeps after ~15 min idle; the first request cold-starts
in ~30–60s. Acceptable for MVP. Optionally add a free uptime pinger hitting `/health`.

## Build phases

Built incrementally; each phase ends with tests + a checkpoint. Backend is verified live
against Neon; the Android app compiles green in CI.

- ✅ **Phase 0 — Bootstrap:** server + `/health`/`/ready`, Android skeleton, CI, render.yaml.
- ✅ **Phase 1 — Identity/Profile/Calc:** calc engine (100% cov), guardrails (100% stmts),
  Argon2id auth + JWT rotation, AES-256-GCM profile, `/calc`.
- ✅ **Phase 2 — Diet plans:** food DB + deterministic `rules` generator + guardrails.
- ✅ **Phase 3 — Logging & progress:** food/water logging, barcode (Open Food Facts),
  weekly check-ins, dashboard aggregates.
- ✅ **Phase 4 — Adaptive/chat:** rules chat dietitian, adaptive engine, reminders.
- ✅ **Phase 5 — Grocery/reports:** grocery list, CSV + PDF reports, gamification badges.
- ✅ **Phase 6 — Family/i18n:** owner-scoped family members (own AI), Hindi i18n, Health
  Connect steps, dark mode.
- ✅ **Phase 7 — Hardening:** rate limiting, request IDs, data export, account deletion.
  (Signed release build remains a manual step — see docs/ANDROID_GUIDE.md.)

Android app: auth, onboarding wizard, dashboard, today/7-day plan, chat coach.

## API surface (`/api/v1`)

`auth/{register,login,refresh,logout,me}` · `profile` · `calc`, `calc/preview`, `calc/latest` ·
`plan`, `plan/latest` · `foods` · `logs/food`, `logs/water`, `checkins`, `dashboard`,
`barcode/:code` · `chat`, `adapt`, `reminders` · `grocery`, `gamification`,
`reports/weekly.{json,csv,pdf}` · `family`, `family/:id/{calc,plan}` · `me/export`, `DELETE /me`.

## License / data

USDA FoodData Central is public domain. Indian food values (IFCT/ICMR-NIN) are subject to
licensing — verify before bundling; otherwise store openly-licensed / derived values and
cite. Barcode data via Open Food Facts.
