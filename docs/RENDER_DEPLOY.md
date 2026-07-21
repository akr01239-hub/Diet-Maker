# Deploying the API to Render (free web service)

Two ways: **Blueprint** (reads `render.yaml`, recommended) or a **manual Web Service**.
Either way you paste the environment variables below. `.env` itself is git-ignored — you set
these values in the Render dashboard, never in the repo.

## Option A — Blueprint (recommended)

1. Render → **New +** → **Blueprint**.
2. Connect the repo `akr01239-hub/Diet-Maker`. Render reads `render.yaml`.
3. `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` are auto-generated. Fill the `sync: false`
   ones (see table below) in the dashboard.
4. Deploy. The build runs: `npm ci && prisma generate && npm run build && prisma migrate deploy`.

## Option B — Manual Web Service

1. Render → **New +** → **Web Service** → connect the repo.
2. **Root Directory:** `server`
3. **Build Command:** `npm ci && npx prisma generate && npm run build && npx prisma migrate deploy`
4. **Start Command:** `npm run start`
5. **Health Check Path:** `/health`
6. Add the environment variables below (Render's env editor has a “Add from .env” paste box).

## Environment variables

| Key | Notes |
|-----|-------|
| `NODE_ENV` | `production` |
| `DATABASE_URL` | Neon **pooled** connection (`...-pooler...` host) |
| `DIRECT_URL` | Neon **direct** connection — same string, host **without** `-pooler` |
| `JWT_ACCESS_SECRET` | 48+ random bytes (auto in Blueprint) |
| `JWT_REFRESH_SECRET` | 48+ random bytes (auto in Blueprint) |
| `HEALTH_DATA_ENCRYPTION_KEY` | 32-byte base64 (`openssl rand -base64 32`) |
| `AI_PROVIDER` | `rules` |
| `USDA_FDC_API_KEY` | your free FDC key (optional) |
| `CORS_ORIGINS` | `*` for now; lock down later |

> **Pooled vs direct:** the app uses the pooled URL at runtime; Prisma migrations use the
> direct URL. On Neon the direct host is the pooled host with `-pooler` removed.

## After it deploys

- `GET https://<service>.onrender.com/health` → `{"status":"ok",...}`
- `GET https://<service>.onrender.com/ready` → `{"status":"ready","db":"up"}`

**Free-tier note:** the service sleeps after ~15 min idle; the first request cold-starts in
~30–60 s. Optionally add a free uptime pinger hitting `/health` to keep it warm.

## Security

The Neon password and any token pasted in chat should be **rotated** (Neon → Roles → reset).
Committed migration files contain no secrets, so a password reset only means updating
`DATABASE_URL`/`DIRECT_URL` in Render.
