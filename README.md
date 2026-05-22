# Kraftshala Offline Attendance System

A deterministic, multi-signal attendance system for Kraftshala's first offline batch.

## Repo Structure

```
kraftshala/
├── architecture.md         # System architecture
├── requirements.md         # FRs/NFRs + schema
├── plan.md                 # Build sequencing
├── README.md               # This file
├── supabase/               # Backend
│   ├── migrations/         # SQL schema
│   ├── functions/          # Deno edge functions
│   └── config.toml         # Project config
├── android/                # Single APK, dual-role
│   └── app/src/main/...    # Kotlin + Compose
└── web/                    # Admin dashboard
    └── app/...             # Next.js
```

## Quick Start

### 1. Backend (Supabase)

```bash
cd kraftshala
npm install -g supabase
supabase login
supabase link --project-ref <YOUR_PROJECT_REF>
supabase db push   # applies migrations
supabase functions deploy --no-verify-jwt
```

### 2. Web dashboard (Next.js)

```bash
cd web
npm install
cp .env.example .env.local   # add SUPABASE keys
npm run dev                  # http://localhost:3000
```

### 3. Android app

```bash
cd android
# Open in Android Studio (Hedgehog or later)
# Fill in local.properties:
#   supabase.url=https://<project>.supabase.co
#   supabase.anonKey=<your_anon_key>
# Build → Run on a real Android 12+ device
```

## The Three Surfaces

| Surface | Stack | Roles |
|---|---|---|
| Android app | Kotlin + Compose | Student + Instructor |
| Web dashboard | Next.js + shadcn | Admin + Coordinator |
| Backend | Supabase (Postgres + Auth + Realtime + Edge Functions) | Shared |

## Demo Flow

1. Two Android phones, one logged in as instructor, one as student
2. Instructor opens session → BLE broadcast begins automatically
3. Student walks into geofence → app detects + auto-prompts mark
4. Student taps "Mark me present" → signed token verified → recorded
5. Instructor sees live roster on phone
6. Session ends → instructor calls absent names, overrides where needed
7. Admin opens web dashboard → sees finalised attendance + anomaly queue
