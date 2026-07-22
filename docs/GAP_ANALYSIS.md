# NutriAI — Gap Analysis & Product Roadmap

_Compiled from a three-track read-only audit (UI/UX, Backend/Architecture, AI/Health-Intelligence)._
_Goal: transform NutriAI from a strong deterministic health app into a premium AI Health Companion._

## Executive summary

| Dimension | Score | One-line verdict |
|---|---|---|
| Backend / domain core | **8.5/10** | Clean, modular, **safety-critical math is genuinely well-tested** (176 unit cases). Debt is at infra/persistence, not domain logic. |
| Feature completeness | **7.5/10** | Strong core; missing a **health-risk engine**, micronutrients, and premium AI experiences. |
| UI / UX | **3/10** | Functional and consistent, but the consistency is copy-paste, not a design system. **No real design system, near-zero accessibility, no skeletons/motion, dark mode effectively broken.** |

**The single biggest levers:** (1) a real **design system + accessibility** pass, (2) a deterministic **AI Health-Risk engine** (all inputs already collected), (3) **micronutrients** in the food model (unlocks deficiency detection), (4) a **proactive coach** surface.

**Constraint that shapes everything:** the app is **100% zero-cost**. Meal-photo and body-photo recognition are the _only_ features that hard-require a paid/quota API (Gemini); everything else in this roadmap is buildable **free & deterministically**.

---

## Part A — UI / UX

### Cross-cutting (read first)
- **No design system.** `theme/Type.kt` is bare `Typography()` (Material defaults). No spacing scale, no shape scale, no shared components. Every screen re-implements its own gradient hero card, paddings, and radii (corner radii `8/10/14/16/18/20/22/24/26/28dp` used ad hoc).
- **Hardcoded brand colors bypass the theme** (`BrandGreen*`, `BrandAmber` used directly instead of `MaterialTheme.colorScheme`). `Theme.kt` defines a full dark scheme but it's **effectively dead** — dark mode will have poor contrast (white text on light-tuned gradients).
- **Accessibility ≈ zero.** Only 3 `contentDescription`s in the whole UI tree. Every emoji-in-a-clickable-Box control (favorite ☆, delete ✕, swap 🔄, recipe 📖, send ➤, mood faces) is invisible to TalkBack. Many touch targets < 48dp. Hardcoded `.sp` sizes + fixed-height cards will clip at large font scale. **P0, systemic.**
- **Emoji-as-iconography** everywhere — inconsistent across OEM font packs, no semantic labels.
- **No skeleton loaders** (bare `CircularProgressIndicator` everywhere; dashboard warns of ~30s cold start behind a spinner). **No pull-to-refresh. No haptics.** Only 2 animations exist app-wide (splash fade, breathing circle).

### Per-screen (priority / complexity)
| Screen | State | Top problems | P / C |
|---|---|---|---|
| Onboarding | 1-page wall of ~15 inputs, 11 dropdowns | **Biggest activation risk.** No multi-step, no progress, no plan-preview payoff, high abandonment | **P0 / L** |
| Dashboard | Richest screen | ~~Macro carbs/fat bar permanently full~~ (FIXED), 3 card radii, no first-run state, no trends/sparklines | P1 / L |
| Nav shell + More | 5 emoji tabs, 9 items under "More" | Hand-rolled nav stack instead of NavController; core features buried 2 levels deep | P1 / M |
| Plan / Calendar | Most overloaded (968 lines) | 6 card styles, spreadsheet feel, no real month view, tiny emoji tap targets | P1 / L |
| Log | Best-executed | No barcode entry here, no edit-after-log, no portion presets, star doesn't reflect state | P2 / M |
| Coach | Chat UI | No timestamps/markdown/retry, static suggestions, no streaming | P2 / M |
| Auth | Clean | No password toggle, no forgot-password, no OAuth | P1 / S |
| Splash | Fade | **Hardcoded 2.2s blocking delay** on every cold start | P1 / S |
| Grocery | Bordered tables | **No check-off boxes** while shopping, no share/export | P1 / M |
| Wellness | Breathing session is the app's best moment | Generic cards, no session history/streak | P2 / M |
| Cycle | Thoughtful content | Buried in the Plan list; no cycle wheel/calendar viz | P1 / M |
| Family | Works | DOB is free-text here vs pickers elsewhere; always-expanded add form | P2 / M |
| Reports | Good error states | No charts (it's a "report" with no visuals), no monthly/quarterly | P2 / S |
| Settings | Thin | No theme toggle, no units (kg/lb), no export, no notif-time customization | P2 / S |
| Badges | 2-col grid | All badges share 🏆/🔒 glyph — undifferentiated | P2 / M |
| Body / Barcode | Well-built | Body: no history (by design). Barcode: no reticle/torch, buried under More | P2 / M |

### Design-system verdict
It does **not exist as a system** — it's a _visual convention_ (green gradients + rounded cards) re-created per file. Building blocks exist (brand palette, M3 theme incl. dark). **Missing:** typography scale, spacing/shape tokens, shared component library (`GradientHero`, `BrandField`, `SectionLabel`, `StatTile`, skeletons), working dark mode, motion, vector icons, accessibility layer.

---

## Part B — Backend / Architecture

The domain/safety core (`calc`, `guardrails`, `cycle`, `foodFilter`) is **production-quality and well-tested**. Gaps are concentrated in persistence-layer testing, data-at-rest consistency, rate-limiting/scale, and unbuilt "ecosystem" surface area.

### Cross-cutting
- **Rate limiting is in-memory/per-process** (`REDIS_URL` provisioned but unused) → breaks under horizontal scaling. **P1/M**
- **Expensive AI endpoints share the global limit** (`/chat`, `/foods/photo`, `/body/assess-photo`, `/recipe`) — one user can burn LLM quota. **P1/S**
- **`requireRole` defined but never used** — no RBAC despite `dietitian`/`admin` roles. **P2/M**
- **AI provider gating inconsistent** — chat respects `AI_PROVIDER`; vision/recipe activate on key-presence only. `ollama` is a dead enum. **P2/S**
- **Unbounded `prisma.food.findMany()` / all-logs scans** per request (fine at 50 rows, not at scale). **P2/M**
- Secrets hygiene **good** (`.env` gitignored, Zod-validated env, helmet, prod error hiding).

### Data-at-rest inconsistencies (highest-value security fixes)
- **`ChatMessage` stored plaintext** — users type health details into coach chat. **P1/M**
- **`PeriodLog` stored plaintext** — reproductive data; inconsistent with the profile's AES-256-GCM encryption. **P1/M**
- **Account delete = hard delete, no confirmation/grace period.** **P1/S**
- **Single encryption key, no key-version** → rotating `HEALTH_DATA_ENCRYPTION_KEY` orphans all ciphertext. **P1/M**
- `AuditLog` has no IP/user-agent. **P2/S**

### Auth gaps
No email verification, no password reset/forgot, no refresh-token **reuse detection**, no 2FA, no "log out everywhere", no OAuth (despite `GOOGLE_CLIENT_ID` env), no account lockout. Refresh tokens never pruned.

### Test coverage
**Strong** on all pure/deterministic logic (19 files, ~176 cases). **Absent** on every DB-touching service (profile, logging, plan, chat, cycle, exercise-log, family, vision, body, recipe, account, reports) and all route handlers except `/calc/preview` + `/health`. **P1/L** — add an integration suite against a Neon test branch.

### Missing "ecosystem" tables
Sleep time-series, wearable/activity data, medication & supplement schedules, lab/biomarker history (currently single BP/glucose value inside profile JSON, not time-series), persisted reminders/notifications, completed wellness-session logs, notification tokens, dietitian↔client linkage.

---

## Part C — AI / Health-Intelligence

The deterministic core is strong; gaps are in **breadth of health intelligence**, not safety.

### Health engine — what's missing
`BMI, BMR, TDEE, BF%, ideal weight, WHtR, macros, water, projection` all ✅. **Missing (all free):** Lean Body Mass (BF% computed but LBM never derived), body water %, **WHR** (`waistCm`/`hipCm` captured but never computed), and — the foundational gap — **micronutrients**: the food model is macro-only (kcal/protein/carb/fat/fiber/sugar/sodium). No vitamin or mineral column exists, so all deficiency advice is generic text, never quantified.

### AI Health-Risk Engine — **the single largest missing pillar**
There is **no risk-scoring engine**. Guardrails _react_ to declared conditions but do not _detect_ risk. Yet the inputs already exist and are unused: `bloodPressure`, `restingHr`, `bloodSugar`, `waistCm`, `hipCm`, `neckCm` (profile) + `sleepHours`, `mood`, `energy`, `pain` (check-ins) + water/food intake. A deterministic engine (WHtR + BMI + waist + BP + fasting glucose → cardiometabolic bands, each with why/confidence/recommendation/next-action) is entirely buildable **free**. Model it on the existing `cycle.ts analyzeCycleHealth`, which is already shaped exactly like a risk engine.

### Other AI capabilities
- **Meal recognition** — partial (macros only, via Gemini; no micros, no confidence, no DB reconciliation). Paid API.
- **Body scanner** — partial (single-photo BF% range via Gemini + free Deurenberg fallback). Missing multi-angle/posture/timeline. **Progress timeline is free** (measurement history already stored).
- **Dynamic diet generator** — adapts to only 2 of 9 target signals (yesterday's intake, workout delta). **Missing free adaptations:** sleep, weight trend, stress, water, mood, budget (`monthlyBudget`/`costTier` defined but unread), pantry.
- **Proactive coach** — pieces exist (nightly adaptation, projections) but no unified morning-insight surface. _(A dashboard coach card now ships — see roadmap #3.)_
- **Per-meal nutrition depth** — sparse; **condition-friendliness scores (diabetes/heart/liver/gut/inflammation)** are a standout free win: all inputs (GI, sugar, sodium, satfat, fiber, fermented tags) already exist per food; only a hard include/exclude filter uses them today.

---

## Prioritized roadmap (zero-cost, deterministic first)

### Now / P0
1. ✅ **SHIPPED — Deterministic Health-Risk Engine** (`modules/risk/`) — central obesity (WHtR), Asian-BMI status, BP, glucose, resting HR, sleep, hydration; each with why/recommendation/next-action. `GET /risk` + dashboard "Your health signals" card.
2. **Accessibility pass** — content descriptions, `semantics`, 48dp targets, font-scale safety. Systemic. _(still open)_
3. ✅ **SHIPPED — Proactive coach** — "🧠 Your AI coach" dashboard card (greeting, sleep/water/log nudges, projection, streak). Deterministic. _(server extension still open)_
4. ✅ **SHIPPED — Per-meal condition-friendliness scores (0–100)** — `friendliness.ts`; diet card shows "💚 Diabetes-friendly" chips.
5. **Onboarding redesign** — multi-step, progress, chips, plan preview (activation-critical). _(still open)_

_Also shipped this cycle: portion normalisation so plans hit the calorie target; sleep score; Google-Fit/Health-Connect connect button on vitals; fixed the carbs/fat macro-bar "always full" bug._

### Next / P1
6. **Design system** — typography/spacing/shape tokens, shared components, skeletons, **make dark mode actually work**.
7. **Micronutrient columns** on the food model + intake tracking (unlocks deficiency risk & vitamin tracking).
8. **WHR + Lean Body Mass** in the health engine (two formulas, inputs already captured).
9. **Encrypt/scrub ChatMessage + PeriodLog; account-delete confirmation; key-versioning.**
10. **Budget- & pantry-aware, sleep/stress-adaptive diet generation** (wire up existing unused fields).
11. **Behavior-triggered nudges** (protein-gap-by-evening, hydration-behind-pace) replacing fixed-clock reminders.
12. **Grocery check-off**, **splash delay removal**, **More→NavController**, **Redis rate-limit + per-AI-endpoint throttle**.

### Later / P2
13. Body-composition **progress timeline** (free; data already stored). Charts in Reports. Badge art. Chat markdown/retry. Barcode reticle/torch. Settings expansion (theme/units/export). DB-service integration tests. De-dupe `toFoodItem` (5 copies). Recipe caching.

### Requires paid API (deferred under zero-cost)
Meal-photo & body-photo recognition (Gemini). Free alternatives: barcode (Open Food Facts) for food entry; Deurenberg for body-fat.

---

_This document is a living audit. Update the "shipped" markers as roadmap items land._
