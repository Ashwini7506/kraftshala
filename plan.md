# Plan — Kraftshala Offline Attendance System

Build sequencing, milestones, decisions, and the order things ship in. Companion to `architecture.md` and `requirements.md`.

---

## 1. The Goal

Ship a working v1 of the attendance system before Kraftshala's first offline batch starts (6 weeks from project kickoff). v1 must handle the 30-learner cohort in one classroom for the full 9-month program duration, with no major incident.

---

## 2. Critical Path

```
Supabase schema + RLS
        │
        ▼
Auth + role routing (shared between Android + Web)
        │
        ▼
Android scaffold + onboarding + device key pair
        │
        ▼
Android BLE broadcast (instructor) + scan (student)
        │
        ▼
Self-mark + signed-token flow + geofence
        │
        ▼
Live roster on instructor screen (Realtime)
        │
        ▼
End-of-session reconciliation + four-flag state machine
        │
        ▼
Web dashboard scaffold + admin views
        │
        ▼
Anomaly queue + audit log search + exports
        │
        ▼
QR re-pairing flow (Android side + web portal side)
        │
        ▼
End-to-end pilot test in a real classroom
```

Each step depends on the previous one. If any step slips, everything downstream slips equally.

---

## 3. Build Order — what ships when

The build is sequenced by dependency, not by calendar week. Each milestone has a clear "done" definition.

### Milestone 1 — Foundation

**Goal**: backend schema is live, mobile and web can authenticate against it.

- Supabase project created (`kraftshala-attendance`)
- Schema deployed (all tables from `requirements.md` section 4)
- Row-level security policies applied
- Supabase Auth configured (email magic link)
- Test users created for each role (student, instructor, admin, coordinator)
- Android project scaffolded with Compose + Supabase Kotlin client
- Web project scaffolded with Next.js + Supabase JS client

**Done when**: a test student can log in on Android, see their profile in the app; a test admin can log in on the web dashboard, see the empty sessions list.

### Milestone 2 — Identity Layer

**Goal**: each device is cryptographically bound to a user.

- Android Keystore key pair generation on first launch
- Public key registration with backend
- Device fingerprint capture (Android ID + install ID + OS metadata)
- One-device-per-user enforcement at backend
- Token signing utility wired in

**Done when**: enrolling a student device writes a row to `devices` with a valid public key, and a second enrollment attempt is rejected.

### Milestone 3 — Proximity Layer

**Goal**: BLE broadcast + scan + geofence all work end-to-end.

- Foreground service for active BLE during a session
- `BluetoothLeAdvertiser` broadcasting session nonce on Kraftshala-reserved UUID (instructor mode)
- `BluetoothLeScanner` listening passively (student mode)
- FusedLocationProvider + Geofencing API for campus boundary check
- BLE proximity event logging to local Room DB, sync to Supabase

**Done when**: instructor's phone broadcasts a session nonce, student's phone within 10m detects it, both events visible in `ble_proximity_log`. Geofence enter/exit events log correctly.

### Milestone 4 — Self-Mark + Signed Token

**Goal**: student can mark themselves present, system verifies the mark cryptographically.

- "Mark me present" button enabled only when geofence + BLE both pass
- Local signing of `(session_id + nonce + timestamp + ble_signal_strengths)` with device private key
- Edge function verifies the signature against stored public key
- Verified marks write to `attendance_records` with self_mark_at populated

**Done when**: a student in the classroom can tap mark, the signed token is verified server-side, and the row is visible in `attendance_records`.

### Milestone 5 — Live Roster + Realtime

**Goal**: instructor sees the live roster updating as students mark in.

- Supabase Realtime subscription on `attendance_records` for the active session
- Instructor's app renders 4 tables: Present, Absent, Present-but-not-at-lecture, Present-but-left-in-between
- Initial state: all enrolled students show as Absent
- As students self-mark and BLE detects them, they move to Present
- BLE drop during session triggers move to Present-but-left-in-between
- Self-mark without BLE detection (e.g., proxy fraud) shows as Present-but-not-at-lecture pending instructor review

**Done when**: across 2 phones (one instructor, one student), marking in shows up on the instructor's screen within 3 seconds.

### Milestone 6 — End-of-Session Reconciliation

**Goal**: instructor closes the session, system writes final flags + audit log.

- "Submit roster" button on instructor screen
- Instructor calls out names from the Absent / Present-but-not-at-lecture tables
- Per-row override controls (tap "Override to present" with optional reason field)
- On submit: all final_flag values lock, audit_log writes one row per change
- Session status flips to `closed`
- LMS push (placeholder webhook for v1, real integration in v1.x)

**Done when**: a session can be closed cleanly, audit log captures every override, and the closed session is immutable.

### Milestone 7 — Offline-First Behaviour

**Goal**: the entire mark-in flow works without internet.

- Local Room schema for offline queue
- All BLE, signing, and self-mark events persist locally first
- Background sync worker that flushes the queue on reconnect
- Sync preserves original timestamps, adds `synced_at` separately
- Conflict handling: if a row already exists server-side, server wins

**Done when**: a 30-minute session run with WiFi off the whole time, then reconnected, results in correct attendance records with original timestamps.

### Milestone 8 — Web Dashboard

**Goal**: Kraftshala admin / coordinator can review and act on attendance data.

- All-sessions view with filters (cohort, date range, campus)
- Per-session detail view (read-only roster mirror)
- Anomaly queue: list of flagged students across all closed sessions
- Per-anomaly resolution actions: view raw signals, override, dismiss, escalate
- Audit log search with filters (actor, action, target, date range)
- CSV exports per session / per learner / per cohort

**Done when**: a coordinator can log in, see the day's flagged anomalies, resolve them, and export a cohort attendance CSV.

### Milestone 9 — Device Re-Pairing

**Goal**: lost-phone recovery without coordinator intervention.

- Web portal page: "Lost device / New phone"
- Web generates a one-time QR pairing code (10-minute expiry, single use)
- Android onboarding screen: "Re-pair existing account"
- QR scanned on new device, new key pair generated, old device revoked atomically
- Frequency cap: max 1 re-pair per 7 days, second attempt blocked
- Multi-pair detection: 3+ re-pairs in a semester auto-flags the account

**Done when**: a learner can scan a QR on a new device and recover their account in under 2 minutes from any computer.

### Milestone 10 — Pilot Test

**Goal**: validate the system in a real Kraftshala classroom with real users.

- Onboard 5-10 internal Kraftshala staff as test learners
- Run 3 mock sessions across 2 days in the actual classroom
- Test conditions: normal, WiFi outage, instructor's phone dies mid-session, late arrivals, group arrivals
- Capture defects and friction
- One round of fixes before Day 1

**Done when**: 3 consecutive mock sessions run without a critical incident, and the team feels comfortable opening it to the real cohort.

---

## 4. Resource Plan

### People

- **1 PM (Ashwini)** — owns spec, schema, integration, testing, and Kraftshala stakeholder management
- **2 engineers** — currently committed to other Kraftshala work. Realistically 50% available = 1.0 FTE equivalent over the 6-week window
- **1 designer** (optional) — for UI polish on Android + web. If unavailable, PM uses shadcn defaults

### Hardware

- Two physical Android phones for testing (instructor + student roles). Pixel + Samsung covers most variance.
- One laptop for web dashboard development.

### Accounts

- Supabase (free tier sufficient for v1)
- Vercel (free tier sufficient)
- GitHub (free tier sufficient)
- No Apple Developer account needed (iOS deferred)
- No Google Play Console needed (sideloaded APK for v1)

### Total cost estimate (v1)

- Supabase free tier: ₹0
- Vercel free tier: ₹0
- Domain (`kraftshala-attendance.in` for QR portal): ~₹800/year
- Test devices: assumed already available
- Engineering time: internal, no contract cost

**v1 build cost: ~₹800 + internal engineering time.**

If engineers can't commit even 50%, fallback is one contract Android engineer for 4 weeks at ~₹2-3L total.

---

## 5. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| BLE behavior varies across Android device variants | High | Medium | Survey phone models in week 1, test on top 5 in week 5 |
| Phone battery drain from continuous BLE | Medium | Medium | BLE only active during scheduled session windows |
| First-day chaos with 30 simultaneous enrollments | Medium | High | Stagger onboarding across the week before Day 1 |
| Instructor's phone dies mid-session | Low | Medium | Printed paper roster as emergency fallback |
| Campus WiFi unstable | High | Low | Architecture is offline-first by design |
| Apple Developer account requirement for any iOS user | Low (v1) | High (v1.1) | iOS deferred; if a cohort member has only iPhone, manual marking via instructor for v1 |
| Supabase free tier limits hit at scale | Low (v1) | Medium | Upgrade to Pro at ₹2,000/month when traffic justifies |
| Proxy fraud detection misses cases | High | Medium | Pattern detection layer added in v2; v1 relies on instructor reconciliation |
| Lost phone fraud (legit students compromised) | Low | High | QR re-pair frequency caps + audit-driven flags + manual review at threshold |

---

## 6. Roadmap Beyond v1

### v1.1 (post-pilot, 2-3 weeks after Day 1)

- Mesh-relay BLE for larger classrooms
- iOS app (after Apple Developer enrollment)
- LMS webhook integration (real, not placeholder)
- In-session random pings for proxy detection
- Coordinator dispute UI improvements

### v1.5 (3 months in)

- Pattern detection on attendance data (statistical anomaly surfacing for coordinator)
- Per-campus geofence support for multi-campus rollout
- Reward/penalty hooks into LMS (attendance-tied policies)
- Bulk operations on web dashboard (mass override, bulk export)

### v2 (6 months in)

- AI-powered anomaly detection (background)
- Natural-language query interface on dashboard
- Cohort comparison analytics
- Predictive at-risk learner flagging
- Multi-language UI (Hindi + regional)

---

## 7. Decisions Locked vs Open

### Locked

- Architecture: 1 Android app + 1 web dashboard + 1 Supabase backend
- Deterministic system, no AI in v1 primary path
- Android-first, iOS deferred
- Instructor's phone is the BLE anchor in v1; mesh relay deferred
- Email magic link for auth
- Postgres + Supabase Realtime for live roster
- Offline-first behaviour mandatory
- Four-flag attendance state machine
- QR-based self-service device re-pairing

### Open (lock by Milestone 1)

- Final LMS push payload spec (need handoff with current LMS team)
- Default geofence radius (provisional: 100m polygon around campus building)
- Late threshold (provisional: 15 minutes past session start = "Late" sub-tag)
- BLE TX power tuning (provisional: default Android value, adjust after pilot)
- Cohort capacity per session (v1 = 30, may need adjustment based on classroom size)

---

## 8. How Success is Measured

End of v1, we measure:

- **Adoption** — every learner in the pilot cohort enrolled with one device
- **Coverage** — % of sessions where BLE + geofence + self-mark all triggered correctly
- **Fraud flags** — number of "Present but not at lecture" entries, trending toward zero as instructors and learners adjust
- **Sync reliability** — % of offline-queued events that synced successfully on reconnect
- **Dispute resolution time** — median time from learner dispute to coordinator resolution
- **Manual overrides** — number per session, trending down over time
- **System uptime** — % of scheduled sessions where the system worked end-to-end without manual intervention

Target after 30 days of live operation: adoption 100%, coverage 95%, fraud flags <5/cohort/week, sync reliability 99%, manual overrides <2 per session.

---

## 9. Submission Artefacts (for Kraftshala assignment)

For the Kraftshala APM assignment, the deliverables are:

- This `architecture.md`, `requirements.md`, `plan.md` set
- The working Android APK (one binary, both roles)
- The web dashboard hosted at a Vercel URL
- A 3-5 minute screen recording demonstrating:
  - Student onboarding + mark-in flow
  - Instructor session start + live roster + end-of-session reconciliation
  - Web dashboard anomaly review + export
  - Offline behaviour (WiFi off, mark-in still works, sync on reconnect)
- GitHub repo link with the codebase
- A short written explanation of the four-flag state machine and the proximity layers

---

## 10. The Philosophy

The system minimizes fraud, doesn't eliminate it. We stack independent signals — cryptographic identity, geofence, BLE proximity, self-mark, instructor reconciliation — each catching a different failure mode. No single signal is decisive; the combination is what makes the system trustworthy.

Where the design pushes back on convention:

- **No facial recognition or liveness.** Easily defeated, privacy-burdensome, and adds cost. Instructor's name-based reconciliation does the human-identity job better.
- **No per-classroom hardware.** Adds setup cost, maintenance burden, and barriers to multi-campus scaling. The instructor's phone is the anchor.
- **No mandatory internet at mark-in time.** WiFi is unreliable on Indian campuses. The system has to work without it.
- **No AI in the v1 primary path.** Deterministic, debuggable, and auditable. AI gets layered in v2 once we have data to train it.

The design's job is to make casual fraud annoying enough that most attackers don't try, and to make systematic fraud detectable retrospectively so the rest get caught. Perfect security is impossible; "good enough that 95% of fraud doesn't bother + the remaining 5% gets caught" is the realistic target.
