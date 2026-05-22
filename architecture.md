# Architecture — Kraftshala Offline Attendance System

A deterministic, multi-signal attendance system for Kraftshala's first offline batch (PGP in AI-LED Marketing). No AI in the primary path. Built to survive flaky WiFi, classroom variance, and proxy fraud.

---

## 1. System Overview

The product has two client surfaces and one backend.

```
┌──────────────────────────────────────────────────────────────────┐
│                       CLIENT SURFACES                            │
│                                                                  │
│  ┌─────────────────────────────────┐  ┌─────────────────────┐    │
│  │  ANDROID APP (single APK)       │  │  WEB DASHBOARD       │    │
│  │  - Student mode                  │  │  - Admin / coord     │    │
│  │  - Instructor mode               │  │  - Cross-cohort view │    │
│  │  - Role detected at login        │  │  - Anomaly queue     │    │
│  │  - BLE + Geofence + Crypto       │  │  - Audit log search  │    │
│  │  - Kotlin + Jetpack Compose      │  │  - Next.js + shadcn  │    │
│  └─────────────────────────────────┘  └─────────────────────┘    │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────┐
│                    SUPABASE BACKEND                              │
│                                                                  │
│  Postgres (single source of truth)                               │
│  - users, devices, cohorts, sessions, attendance_records,        │
│    ble_proximity_log, device_pairings, audit_log                 │
│                                                                  │
│  Supabase Auth (email magic link)                                │
│  - student, instructor, admin, coordinator roles                 │
│                                                                  │
│  Supabase Realtime (websockets)                                  │
│  - live roster updates on instructor and dashboard surfaces      │
│                                                                  │
│  Supabase Edge Functions (Deno)                                  │
│  - session nonce generation                                      │
│  - signature verification                                        │
│  - QR re-pairing token issuance                                  │
│  - audit log write (tamper-evident hashing)                      │
│                                                                  │
│  Row-Level Security                                              │
│  - student sees own attendance only                              │
│  - instructor sees cohort                                        │
│  - admin / coordinator sees all                                  │
└──────────────────────────────────────────────────────────────────┘
```

The Android app handles in-room actions (BLE broadcast, BLE scan, geofence, mark-in). The web dashboard handles ops actions (review, search, export). Both read and write the same Postgres tables via Supabase. No direct app-to-app communication — the database is the bus.

---

## 2. Identity Layer

Every device is cryptographically bound to one user.

### At onboarding

- The Android app generates an **ECDSA key pair** locally using the **Android Keystore**.
- The private key never leaves the device. It's hardware-backed where the phone supports it.
- The public key is sent to the backend and stored in the `devices` table along with the device fingerprint (Android ID + install ID + OS metadata).
- One device per user is enforced at the backend. A second enrollment attempt is blocked.

### Per session

- The backend generates a **fresh session nonce** when the session starts.
- The nonce is broadcast over BLE by the instructor's phone (and any future mesh-relay anchors).
- The student's phone receives the nonce, signs `(session_id + nonce + timestamp + ble_signal_strengths)` with its private key.
- The signed token is sent to the backend, which verifies the signature against the stored public key.

### What this prevents

| Attack | Defence |
|---|---|
| Replay (yesterday's token today) | Nonce changes per session |
| Forgery (manufactured token) | Signature can only come from the private key |
| Cloning across devices | Private key is hardware-bound, can't be exported |
| Token-sharing across phones | Token payload includes BLE signal strengths, only validates from inside the room |

### What this does not prevent

- The legitimate physical phone in someone else's hands. That's the job of the instructor's name-based reconciliation at end of session.

### Lost-phone recovery

The student logs into the Kraftshala web portal from any computer (email + password), generates a one-time QR pairing code (valid 10 minutes, single-use), and scans it on the new phone. The new device generates its own key pair, the old device is revoked instantly, and the entire chain is logged in `device_pairings`. Frequency caps and audit-driven flags prevent abuse.

---

## 3. Proximity Layer

Two independent location signals, both offline-capable.

### Geofence (GPS)

The Kraftshala campus is defined as a polygon in the backend. The Android app uses the `FusedLocationProvider` to check whether the learner is inside the polygon when they attempt to mark in. This is the outer ring — proves they are on premises.

### BLE proximity

When a session begins, the instructor's phone runs the Kraftshala app in instructor mode. The app uses Android's `BluetoothLeAdvertiser` to broadcast a BLE advertisement carrying the day's session nonce on a Kraftshala-specific UUID.

Student phones, scheduled into that session via the backend's pub-sub model, run `BluetoothLeScanner` in the background and passively listen for broadcasts matching their subscribed session. No pairing. No user action required.

### Boundary-relay mesh (v1.x roadmap)

For classrooms larger than the natural BLE range of a single phone, the system can promote phones at the edge of the instructor's range — those receiving the weakest signal that still resolves — to also broadcast the session nonce. Their broadcasts extend coverage outward, creating a self-organising mesh that scales with room size and seat layout. No advance configuration. No additional hardware.

In v1 we ship with the instructor's phone as the single anchor. Mesh relay is documented and architected but deferred to v1.1 once we validate the single-anchor model in real rooms.

---

## 4. Attendance State Machine

Three independent signals combine into one of four flags.

| Signal | Source |
|---|---|
| **Self-mark** | Student taps "Mark me present" in the app |
| **BLE proximity** | App logs continuous detection events of the session nonce throughout the session |
| **Instructor name-call** | At end of session, instructor calls out names from the Absent and "Present but not at lecture" tables; learners in the room respond, instructor taps override |

### The four flags

| Flag | Self-mark | BLE proximity throughout | Instructor name-call |
|---|---|---|---|
| **Present** | Yes | Yes, continuous | Not called absent |
| **Absent** | No | Never detected | Called absent (or system inferred) |
| **Present but not at lecture** | Yes | Yes (phone was there) | Called absent (instructor doesn't see them) |
| **Present but left in between** | Yes | Broke mid-session (phone left the room) | Not called absent (instructor missed it) |

Reconciliation happens at session end when the instructor submits. The system writes the final flag to the `attendance_records` table and pushes the record into the LMS (or surfaces it via the web dashboard for now).

---

## 5. Offline-First Behaviour

The entire attendance flow works without WiFi.

- **BLE** is peer-to-peer Bluetooth and needs no internet.
- **Geofence** uses GPS satellites, no internet required.
- **Token signing** happens locally with the on-device key.
- **Self-mark** writes to local Room storage immediately and surfaces "Marked present, will sync when online" in the UI.
- **Instructor's roster** maintains a local copy and updates in real time even with no internet — Realtime falls back to local-only mode.
- **Sync on reconnect**: when WiFi returns, all queued events flush to Supabase. Original timestamps are preserved, not sync timestamps.

The LMS is the source of truth but is never in the critical path of marking attendance.

**Worst-case fallback**: WiFi down AND instructor's phone dies during the session. A printed paper roster is kept in every classroom's emergency folder. The coordinator enters the data into the dashboard the next morning.

---

## 6. Audit + Compliance

Every action writes to an immutable `audit_log` table.

- Each row is hash-chained against the previous row for tamper-evidence.
- PII access is logged.
- Device re-pairing events are logged.
- Instructor overrides are logged with the original system flag, the override, and the reason.
- Coordinator dispute resolutions are logged with the resolution and the resolver.

Quarterly compliance reports auto-generated from the audit log.

---

## 7. Tech Stack — Per Component

### Android app (single APK, dual-role)

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| Navigation | androidx.navigation:navigation-compose |
| BLE | BluetoothLeAdvertiser + BluetoothLeScanner (Android SDK built-in) |
| Location | com.google.android.gms:play-services-location (FusedLocation + Geofencing) |
| Crypto | AndroidKeyStore (ECDSA P-256) |
| Local DB | androidx.room (offline queue + device state) |
| Backend client | io.github.jan-tennert.supabase:supabase-kt (postgrest, auth, realtime) |
| QR scanning | com.journeyapps:zxing-android-embedded |
| Background work | androidx.work:work-runtime-ktx + Foreground Service for BLE |
| Async | kotlinx-coroutines + Flow |
| Logging | timber |

### Web dashboard (admin / coordinator)

| Layer | Technology |
|---|---|
| Framework | Next.js 14+ App Router |
| Language | TypeScript |
| UI | shadcn/ui + Tailwind |
| Backend client | @supabase/supabase-js |
| Auth | Supabase Auth (same provider as mobile) |
| Hosting | Vercel free tier |

### Backend

| Layer | Technology |
|---|---|
| Database | Postgres 16 (managed by Supabase) |
| Auth | Supabase Auth (email magic link) |
| Realtime | Supabase Realtime (websockets, used for live roster) |
| Compute | Supabase Edge Functions (Deno) |
| Security | Row-Level Security on every table |

---

## 8. Why this architecture

- **No additional hardware** in classrooms. No Raspberry Pi, no BLE beacons, no kiosks. Zero per-classroom setup cost.
- **No iOS in v1.** Apple Developer enrollment + provisioning is too heavy for the 6-week window. Android-first.
- **No AI in the primary path.** Deterministic signals only. AI can be layered on later for anomaly detection if needed.
- **No WiFi dependency at mark-in time.** Every critical step works offline. Sync is async.
- **Single database** for both surfaces. No data sync issues between mobile and web — both read the same Postgres.
- **Cryptographic identity + visual reconciliation.** The hard parts (replay, forgery, cloning) are solved with crypto. The soft parts (right human at the phone) are solved with the instructor's eyes.

---

## 9. What's NOT in this Architecture (and Why)

| Decision | Rationale |
|---|---|
| Native iOS app | Defer to v1.1. Apple provisioning, certificates, and TestFlight add days. Android first. |
| WebBluetooth | Browser BLE is unreliable across Android Chrome versions. Native BLE is rock-solid. |
| Facial recognition / liveness | Easily defeated (deepfakes, lookalikes). Replaced by instructor's name-based reconciliation. |
| Per-classroom hardware (Pi, beacons) | Adds setup cost, maintenance burden, and barrier to scaling across campuses. The instructor's phone covers the anchor role. |
| Real-time AI fraud detection | Deferred. Pattern detection on attendance data is a v2 add-on, not v1 critical path. |
| Push notifications via FCM | Defer. Supabase Realtime + in-app notifications cover v1 needs. |
| Standalone instructor laptop view | The instructor app on phone already shows the roster. Laptop is for the admin / coordinator role, served by the web dashboard. |
