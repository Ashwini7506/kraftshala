# Requirements — Kraftshala Offline Attendance System

Functional and non-functional requirements for the v1 build. Aligned to the architecture in `architecture.md`.

---

## 1. Functional Requirements

### 1.1 Android App — Student Mode

| FR-ID | Requirement | Acceptance |
|---|---|---|
| FR-S1 | System SHALL authenticate students via Supabase Auth (email magic link) | One-tap sign-in from the link. No password required. |
| FR-S2 | System SHALL generate an ECDSA key pair locally on first launch and register the public key with the backend | Key pair lives in Android Keystore. Public key + device fingerprint stored in `devices` table. |
| FR-S3 | System SHALL enforce one enrolled device per user | Second enrollment attempt blocked until old device is de-paired via QR flow. |
| FR-S4 | System SHALL detect the campus geofence using FusedLocationProvider | When learner is inside the polygon, attendance flow becomes available. |
| FR-S5 | System SHALL passively scan for BLE broadcasts on the Kraftshala session UUID matching the learner's scheduled sessions | Detection happens without user action when session is active. |
| FR-S6 | System SHALL allow the learner to tap "Mark me present" once both geofence and BLE proximity are confirmed | UI shows the button as enabled only when both signals pass. |
| FR-S7 | System SHALL sign the session nonce locally with the device's private key | Signature includes session_id, nonce, timestamp, and BLE signal strengths. |
| FR-S8 | System SHALL operate offline | Queued marks persist in Room local DB and sync to Supabase on reconnect with original timestamps. |
| FR-S9 | System SHALL continue logging BLE proximity events throughout the session | Disconnect events recorded with timestamps for "left in between" detection. |
| FR-S10 | System SHALL show the learner their attendance record per session with the assigned flag | Visible in app within 1 minute of session end (online) or after sync (offline). |
| FR-S11 | System SHALL allow the learner to dispute a flag | Dispute routed to coordinator queue, status visible in app. |
| FR-S12 | System SHALL allow re-pairing of a new device via a QR scan flow | QR generated from the web portal, scanned in the new device's onboarding screen. Old device revoked immediately upon successful pairing. |

### 1.2 Android App — Instructor Mode

| FR-ID | Requirement | Acceptance |
|---|---|---|
| FR-I1 | System SHALL authenticate instructors via Supabase Auth | Same provider, different role. |
| FR-I2 | System SHALL auto-start BLE broadcasting at the scheduled session start time | Schedule pulled from backend, no manual trigger required. |
| FR-I3 | System SHALL broadcast on a Kraftshala-reserved UUID with the session nonce as payload | Other apps cannot pick up the broadcast meaningfully. |
| FR-I4 | System SHALL stop broadcasting at session end | Defined by `sessions.end_at` plus a small grace period. |
| FR-I5 | System SHALL surface a live roster of all enrolled learners for the session, sorted into four tables | Tables: Present, Absent, Present-but-not-at-lecture, Present-but-left-in-between. |
| FR-I6 | System SHALL update the roster in real time as learners self-mark and as BLE detections come in | Supabase Realtime drives updates. |
| FR-I7 | System SHALL allow the instructor to call out names from the Absent and "Present but not at lecture" tables and override individual flags | One tap to override per learner. Override is logged with timestamp + reason field. |
| FR-I8 | System SHALL submit the final roster at session end | Instructor taps Submit, all flags lock, audit log writes, LMS push triggered. |
| FR-I9 | System SHALL run a foreground service during active BLE sessions | Mandatory for continuous BLE on Android 12+. Notification visible. |

### 1.3 Web Dashboard — Admin / Coordinator

| FR-ID | Requirement | Acceptance |
|---|---|---|
| FR-W1 | System SHALL authenticate admins and coordinators via Supabase Auth with role enforcement | Magic link or password-based, admin role gates dashboard access. |
| FR-W2 | Dashboard SHALL show all sessions across cohorts with key metrics | Per session: total enrolled, present count, absent count, flagged count. |
| FR-W3 | Dashboard SHALL surface an anomaly queue with flagged students for review | Flagged = "Present but not at lecture" or "Present but left in between" or learner-disputed entries. |
| FR-W4 | Dashboard SHALL allow the coordinator to resolve flagged entries | Each entry: view raw signals, override, dismiss, escalate. Resolution logged. |
| FR-W5 | Dashboard SHALL provide a searchable audit log | Filterable by user, action, timestamp range, session. |
| FR-W6 | Dashboard SHALL allow CSV export of attendance per session, per learner, per cohort | One-click export. |
| FR-W7 | Dashboard SHALL show device re-pairing history per learner | Includes IP of QR request, timestamp, frequency flags. |

### 1.4 Cross-Surface

| FR-ID | Requirement | Acceptance |
|---|---|---|
| FR-X1 | System SHALL maintain an immutable audit log of all attendance-affecting actions | Each row hash-chained for tamper evidence. |
| FR-X2 | System SHALL enforce role-based access control via Postgres RLS | Students see own data; instructors see their cohort; admins see all. |
| FR-X3 | System SHALL push finalised attendance records to the existing Kraftshala LMS via API | Webhook or push at session-submit time. (v1.x — for v1, the web dashboard is the consumption surface.) |

---

## 2. Non-Functional Requirements

### 2.1 Performance

| NFR-ID | Requirement | Threshold |
|---|---|---|
| NFR-P1 | Mark-in flow end-to-end (tap → confirmation) | ≤ 2 seconds on a typical Android phone |
| NFR-P2 | Roster live update latency | ≤ 3 seconds from learner self-mark to instructor screen |
| NFR-P3 | BLE broadcast advertisement interval | 1-second interval |
| NFR-P4 | Geofence trigger response | ≤ 5 seconds from entering polygon |
| NFR-P5 | Web dashboard initial load | ≤ 2 seconds on broadband |
| NFR-P6 | Cohort dashboard render with 30 active learners | ≤ 1 second after data fetch |

### 2.2 Reliability + Offline

| NFR-ID | Requirement | Threshold |
|---|---|---|
| NFR-R1 | Attendance marking SHALL work fully without internet | 100% offline-capable for mark-in. Sync queued on reconnect. |
| NFR-R2 | Queued events SHALL retain original timestamps after sync | Server stores both `event_at` and `synced_at`. |
| NFR-R3 | App SHALL survive a full session with no network | Buffer up to a 4-hour session worth of events locally. |
| NFR-R4 | Supabase availability | 99.9% (Supabase SLA) |

### 2.3 Security

| NFR-ID | Requirement | Implementation |
|---|---|---|
| NFR-S1 | All data in transit | TLS 1.3 |
| NFR-S2 | Device key pair | ECDSA P-256, hardware-backed where supported |
| NFR-S3 | Postgres row-level security | Enforced on every table |
| NFR-S4 | Audit log | Append-only, tamper-evident hash chain |
| NFR-S5 | QR re-pairing | One-time, 10-min expiry, frequency-capped per user |
| NFR-S6 | Authentication | Supabase Auth, JWT-based session, 8-hour expiry for student role, longer for admin |
| NFR-S7 | DPDP compliance | Granular consent at onboarding, right to erasure, no PII in audit metadata |

### 2.4 Scalability

| NFR-ID | Requirement | v1 Target | Full Scale |
|---|---|---|---|
| NFR-SC1 | Concurrent learners per session | 30 | 100 per session (auditorium support) |
| NFR-SC2 | Total enrolled learners | 30 (one cohort) | 5,000 (across cohorts and campuses) |
| NFR-SC3 | Sessions per day | 5 | 100+ |
| NFR-SC4 | Concurrent active sessions | 2 | 30 |

### 2.5 Accessibility

| NFR-ID | Requirement | Standard |
|---|---|---|
| NFR-A1 | Student app + instructor app | WCAG 2.1 AA for color contrast, type sizing |
| NFR-A2 | Web dashboard | WCAG 2.1 AA |
| NFR-A3 | Keyboard navigation on web | All interactive elements reachable via Tab |
| NFR-A4 | Mobile UI | Large tap targets (≥48dp), readable type at 14sp minimum |

### 2.6 Device Support (Android)

| NFR-ID | Requirement |
|---|---|
| NFR-D1 | Minimum Android version: 12 (API 31) — needed for new BLE permissions model |
| NFR-D2 | Target Android version: 14 (API 34) — latest stable |
| NFR-D3 | Tested on: Pixel, Samsung Galaxy A-series, Xiaomi Redmi (top 3 phone families in Indian Tier 2/3 student demographic) |
| NFR-D4 | Minimum BLE: Bluetooth 4.0 (BLE) — every Android 12+ phone supports this |
| NFR-D5 | Web dashboard tested on: Chrome, Safari, Firefox (last 2 versions each) |

---

## 3. Compliance

| ID | Requirement |
|---|---|
| C1 | Full compliance with India's Digital Personal Data Protection Act 2023 |
| C2 | Explicit, granular, revocable consent capture at onboarding |
| C3 | Right to erasure with cascading deletion across all tables |
| C4 | Immutable, tamper-evident audit log |
| C5 | No data sold or shared with third parties without explicit per-purpose consent |
| C6 | Per-tenant data isolation (each cohort is its own logical scope) |
| C7 | Anti-discrimination audit on attendance flagging (no demographic skew in "Present but not at lecture" rates) |
| C8 | Right to data portability (CSV export of full learner attendance history within 24 hours of request) |

---

## 4. Database Schema (Postgres)

### Tables

```sql
-- Users (Supabase Auth populates auth.users; this is our profile extension)
CREATE TABLE users (
  id            UUID PRIMARY KEY REFERENCES auth.users(id),
  full_name     TEXT NOT NULL,
  email         TEXT NOT NULL UNIQUE,
  role          TEXT NOT NULL CHECK (role IN ('student','instructor','admin','coordinator')),
  cohort_id     UUID REFERENCES cohorts(id),
  created_at    TIMESTAMPTZ DEFAULT now()
);

-- Devices (one row per enrolled device, one device per user)
CREATE TABLE devices (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES users(id),
  public_key          TEXT NOT NULL,
  device_fingerprint  TEXT NOT NULL,
  status              TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','revoked')),
  enrolled_at         TIMESTAMPTZ DEFAULT now(),
  revoked_at          TIMESTAMPTZ,
  UNIQUE (user_id, status) -- only one active device per user
);

-- Cohorts
CREATE TABLE cohorts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            TEXT NOT NULL,
  campus_id       UUID REFERENCES campuses(id),
  start_date      DATE,
  end_date        DATE
);

-- Campuses (geofence polygon)
CREATE TABLE campuses (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            TEXT NOT NULL,
  city            TEXT NOT NULL,
  geofence_geojson JSONB NOT NULL
);

-- Sessions
CREATE TABLE sessions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cohort_id       UUID NOT NULL REFERENCES cohorts(id),
  instructor_id   UUID NOT NULL REFERENCES users(id),
  classroom       TEXT NOT NULL,
  start_at        TIMESTAMPTZ NOT NULL,
  end_at          TIMESTAMPTZ NOT NULL,
  session_nonce   TEXT NOT NULL,
  status          TEXT DEFAULT 'scheduled' CHECK (status IN ('scheduled','active','closed'))
);

-- Attendance records (one row per student per session)
CREATE TABLE attendance_records (
  id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id             UUID NOT NULL REFERENCES sessions(id),
  user_id                UUID NOT NULL REFERENCES users(id),
  self_mark_at           TIMESTAMPTZ,
  ble_first_detected_at  TIMESTAMPTZ,
  ble_last_detected_at   TIMESTAMPTZ,
  ble_detected_throughout BOOLEAN DEFAULT FALSE,
  instructor_called_absent BOOLEAN DEFAULT FALSE,
  instructor_override     TEXT,
  final_flag             TEXT CHECK (final_flag IN ('Present','Absent','PresentButNotAtLecture','PresentButLeftInBetween')),
  signed_token           TEXT,
  signature_verified     BOOLEAN DEFAULT FALSE,
  finalised_at           TIMESTAMPTZ,
  UNIQUE (session_id, user_id)
);

-- BLE proximity log (event stream)
CREATE TABLE ble_proximity_log (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id      UUID NOT NULL REFERENCES sessions(id),
  user_id         UUID NOT NULL REFERENCES users(id),
  signal_strength INTEGER, -- RSSI in dBm
  detected_at     TIMESTAMPTZ NOT NULL,
  event_type      TEXT CHECK (event_type IN ('detected','lost'))
);

-- Device pairings (re-pairing history)
CREATE TABLE device_pairings (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES users(id),
  old_device_id       UUID REFERENCES devices(id),
  new_device_id       UUID REFERENCES devices(id),
  qr_token            TEXT NOT NULL,
  request_ip          INET,
  request_at          TIMESTAMPTZ DEFAULT now(),
  paired_at           TIMESTAMPTZ,
  fraud_flag          TEXT -- 'frequency_violation' | 'behavioral_anomaly' | null
);

-- Audit log (immutable, hash-chained)
CREATE TABLE audit_log (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id        UUID REFERENCES users(id),
  action          TEXT NOT NULL,
  target_table    TEXT NOT NULL,
  target_id       UUID,
  payload         JSONB,
  prev_hash       TEXT,
  row_hash        TEXT NOT NULL,
  created_at      TIMESTAMPTZ DEFAULT now()
);
```

### Row-Level Security policies (key examples)

- `students` can SELECT own `attendance_records`; INSERT through Edge Function only
- `instructors` can SELECT `attendance_records` for sessions where `instructor_id = auth.uid()`
- `admins` and `coordinators` can SELECT and UPDATE all rows in `attendance_records`
- `audit_log` is INSERT-only via Edge Function; no UPDATE or DELETE permitted

---

## 5. External Dependencies

| Service | Used For | Critical? |
|---|---|---|
| Supabase | Backend (Postgres, Auth, Realtime, Edge Functions) | Yes — core dependency |
| Vercel | Web dashboard hosting | Yes |
| Google Play Services | FusedLocation + Geofencing | Yes — required for Android location |
| GitHub | Code hosting + CI | Yes |
| Apple App Store / Google Play | NOT used in v1 (sideloaded APK for demo) | No |

---

## 6. Permissions Required

### Android manifest

- `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` (Android 12+)
- `BLUETOOTH`, `BLUETOOTH_ADMIN` (legacy fallback)
- `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- `CAMERA` (QR scanning)
- `POST_NOTIFICATIONS` (Android 13+)
- `INTERNET`, `ACCESS_NETWORK_STATE`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`

### Web dashboard

No special permissions. Standard browser auth.

---

## 7. Open Decisions (to lock in build kickoff)

| Decision | Options | Lock-by |
|---|---|---|
| Auth method | Email magic link (default) vs phone OTP | Day 1 |
| LMS push API spec | Webhook payload format (to integrate with existing Kraftshala LMS) | Week 2 |
| Reward / penalty policy for attendance | Coordinator review at Day 30 | Week 4 |
| Mesh-relay activation criteria | What signal threshold promotes a phone to relay | v1.1 (post-pilot data) |
