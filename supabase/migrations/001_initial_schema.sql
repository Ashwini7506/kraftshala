-- Kraftshala Attendance System — Initial Schema
-- Postgres 16, Supabase
-- All Kraftshala tables live in the `kraftshala` schema to avoid collisions
-- with Sunstone (which shares the same Supabase project, public schema).

-- ============================================================
-- SCHEMA + EXTENSIONS
-- ============================================================
create schema if not exists kraftshala;
set search_path to kraftshala, public;

create extension if not exists "uuid-ossp" with schema public;
create extension if not exists "pgcrypto" with schema public;

-- ============================================================
-- TABLES
-- ============================================================

-- Campuses (each has a geofence polygon)
create table kraftshala.campuses (
  id              uuid primary key default gen_random_uuid(),
  name            text not null,
  city            text not null,
  geofence_geojson jsonb not null,
  created_at      timestamptz default now()
);

-- Cohorts (one batch of learners)
create table kraftshala.cohorts (
  id              uuid primary key default gen_random_uuid(),
  name            text not null,
  campus_id       uuid references kraftshala.campuses(id) not null,
  start_date      date not null,
  end_date        date not null,
  created_at      timestamptz default now()
);

-- Users (extends auth.users with profile + role)
create table kraftshala.users (
  id            uuid primary key references auth.users(id) on delete cascade,
  full_name     text not null,
  email         text not null unique,
  role          text not null check (role in ('student','instructor','admin','coordinator')),
  cohort_id     uuid references kraftshala.cohorts(id),
  created_at    timestamptz default now()
);

-- Devices (one active device per user)
create table kraftshala.devices (
  id                  uuid primary key default gen_random_uuid(),
  user_id             uuid not null references kraftshala.users(id) on delete cascade,
  public_key          text not null,
  device_fingerprint  text not null,
  status              text not null default 'active' check (status in ('active','revoked')),
  enrolled_at         timestamptz default now(),
  revoked_at          timestamptz
);
create unique index kraftshala_one_active_device_per_user
  on kraftshala.devices (user_id) where status = 'active';

-- Sessions (scheduled classes)
create table kraftshala.sessions (
  id              uuid primary key default gen_random_uuid(),
  cohort_id       uuid not null references kraftshala.cohorts(id),
  instructor_id   uuid not null references kraftshala.users(id),
  classroom       text not null,
  start_at        timestamptz not null,
  end_at          timestamptz not null,
  session_nonce   text not null,
  status          text default 'scheduled' check (status in ('scheduled','active','closed')),
  created_at      timestamptz default now()
);
create index kraftshala_sessions_cohort_start on kraftshala.sessions (cohort_id, start_at);
create index kraftshala_sessions_status on kraftshala.sessions (status);

-- Attendance records (one row per student per session)
create table kraftshala.attendance_records (
  id                       uuid primary key default gen_random_uuid(),
  session_id               uuid not null references kraftshala.sessions(id) on delete cascade,
  user_id                  uuid not null references kraftshala.users(id),
  self_mark_at             timestamptz,
  ble_first_detected_at    timestamptz,
  ble_last_detected_at     timestamptz,
  ble_detected_throughout  boolean default false,
  instructor_called_absent boolean default false,
  instructor_override      text,
  override_reason          text,
  final_flag               text check (final_flag in ('Present','Absent','PresentButNotAtLecture','PresentButLeftInBetween')),
  signed_token             text,
  signature_verified       boolean default false,
  finalised_at             timestamptz,
  unique (session_id, user_id)
);
create index kraftshala_attendance_session on kraftshala.attendance_records (session_id);
create index kraftshala_attendance_user on kraftshala.attendance_records (user_id);
create index kraftshala_attendance_flag on kraftshala.attendance_records (final_flag);

-- BLE proximity log (stream of detection events)
create table kraftshala.ble_proximity_log (
  id              uuid primary key default gen_random_uuid(),
  session_id      uuid not null references kraftshala.sessions(id) on delete cascade,
  user_id         uuid not null references kraftshala.users(id),
  signal_strength integer,
  detected_at     timestamptz not null,
  event_type      text check (event_type in ('detected','lost'))
);
create index kraftshala_ble_session_user on kraftshala.ble_proximity_log (session_id, user_id, detected_at);

-- Device pairings (re-pairing audit trail)
create table kraftshala.device_pairings (
  id                  uuid primary key default gen_random_uuid(),
  user_id             uuid not null references kraftshala.users(id),
  old_device_id       uuid references kraftshala.devices(id),
  new_device_id       uuid references kraftshala.devices(id),
  qr_token            text not null,
  qr_expires_at       timestamptz not null,
  request_ip          inet,
  request_at          timestamptz default now(),
  paired_at           timestamptz,
  fraud_flag          text
);
create index kraftshala_device_pairings_user on kraftshala.device_pairings (user_id, request_at);

-- Audit log (append-only, hash-chained)
create table kraftshala.audit_log (
  id              uuid primary key default gen_random_uuid(),
  actor_id        uuid references kraftshala.users(id),
  action          text not null,
  target_table    text not null,
  target_id       uuid,
  payload         jsonb,
  prev_hash       text,
  row_hash        text not null,
  created_at      timestamptz default now()
);
create index kraftshala_audit_actor on kraftshala.audit_log (actor_id, created_at);
create index kraftshala_audit_target on kraftshala.audit_log (target_table, target_id);

-- ============================================================
-- ROW-LEVEL SECURITY (RLS)
-- ============================================================

alter table kraftshala.users enable row level security;
alter table kraftshala.devices enable row level security;
alter table kraftshala.cohorts enable row level security;
alter table kraftshala.campuses enable row level security;
alter table kraftshala.sessions enable row level security;
alter table kraftshala.attendance_records enable row level security;
alter table kraftshala.ble_proximity_log enable row level security;
alter table kraftshala.device_pairings enable row level security;
alter table kraftshala.audit_log enable row level security;

-- Helper functions
create or replace function kraftshala.current_user_role()
returns text language sql stable as $$
  select role from kraftshala.users where id = auth.uid();
$$;

create or replace function kraftshala.current_user_cohort()
returns uuid language sql stable as $$
  select cohort_id from kraftshala.users where id = auth.uid();
$$;

-- USERS policies
create policy "ks users self read" on kraftshala.users
  for select using (id = auth.uid());
create policy "ks users admin all" on kraftshala.users
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- DEVICES policies
create policy "ks devices self all" on kraftshala.devices
  for all using (user_id = auth.uid());
create policy "ks devices admin all" on kraftshala.devices
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- COHORTS / CAMPUSES (read-only for authenticated)
create policy "ks cohorts read all" on kraftshala.cohorts
  for select using (auth.uid() is not null);
create policy "ks cohorts admin all" on kraftshala.cohorts
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

create policy "ks campuses read all" on kraftshala.campuses
  for select using (auth.uid() is not null);
create policy "ks campuses admin all" on kraftshala.campuses
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- SESSIONS policies
create policy "ks sessions student read own cohort" on kraftshala.sessions
  for select using (cohort_id = kraftshala.current_user_cohort());
create policy "ks sessions instructor read own" on kraftshala.sessions
  for select using (instructor_id = auth.uid());
create policy "ks sessions instructor update own" on kraftshala.sessions
  for update using (instructor_id = auth.uid());
create policy "ks sessions admin all" on kraftshala.sessions
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- ATTENDANCE policies
create policy "ks attendance student read own" on kraftshala.attendance_records
  for select using (user_id = auth.uid());
create policy "ks attendance instructor cohort read" on kraftshala.attendance_records
  for select using (
    session_id in (select id from kraftshala.sessions where instructor_id = auth.uid())
  );
create policy "ks attendance instructor cohort write" on kraftshala.attendance_records
  for all using (
    session_id in (select id from kraftshala.sessions where instructor_id = auth.uid())
  );
create policy "ks attendance admin all" on kraftshala.attendance_records
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- BLE LOG policies
create policy "ks ble log student own write" on kraftshala.ble_proximity_log
  for insert with check (user_id = auth.uid());
create policy "ks ble log instructor read" on kraftshala.ble_proximity_log
  for select using (
    session_id in (select id from kraftshala.sessions where instructor_id = auth.uid())
  );
create policy "ks ble log admin all" on kraftshala.ble_proximity_log
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- DEVICE PAIRINGS policies
create policy "ks pairings self read" on kraftshala.device_pairings
  for select using (user_id = auth.uid());
create policy "ks pairings admin all" on kraftshala.device_pairings
  for all using (kraftshala.current_user_role() in ('admin','coordinator'));

-- AUDIT LOG policies
create policy "ks audit read admin" on kraftshala.audit_log
  for select using (kraftshala.current_user_role() in ('admin','coordinator'));

-- ============================================================
-- TRIGGERS
-- ============================================================

create or replace function kraftshala.finalise_attendance_flag()
returns trigger language plpgsql as $$
begin
  if new.finalised_at is null then
    return new;
  end if;

  if new.instructor_override is not null then
    if new.instructor_override = 'force_present' then
      new.final_flag := 'Present';
    elsif new.instructor_override = 'force_absent' then
      new.final_flag := 'Absent';
    end if;
  elsif new.self_mark_at is not null and new.ble_detected_throughout and not new.instructor_called_absent then
    new.final_flag := 'Present';
  elsif new.self_mark_at is null and new.ble_first_detected_at is null then
    new.final_flag := 'Absent';
  elsif new.self_mark_at is not null and new.instructor_called_absent then
    new.final_flag := 'PresentButNotAtLecture';
  elsif new.self_mark_at is not null and not new.ble_detected_throughout then
    new.final_flag := 'PresentButLeftInBetween';
  else
    new.final_flag := 'Absent';
  end if;

  return new;
end;
$$;

create trigger trg_ks_finalise_attendance_flag
before insert or update on kraftshala.attendance_records
for each row execute function kraftshala.finalise_attendance_flag();

-- ============================================================
-- EXPOSE SCHEMA TO POSTGREST
-- ============================================================
-- Grant usage so the Supabase REST API can see the kraftshala schema
grant usage on schema kraftshala to anon, authenticated, service_role;
grant select, insert, update, delete on all tables in schema kraftshala to authenticated;
grant select, insert, update, delete on all tables in schema kraftshala to service_role;
grant execute on all functions in schema kraftshala to authenticated, service_role;

-- ============================================================
-- SEED DATA
-- ============================================================

insert into kraftshala.campuses (id, name, city, geofence_geojson) values (
  '00000000-0000-0000-0000-000000000001',
  'Kraftshala Gurugram Campus',
  'Gurugram',
  '{"type":"Polygon","coordinates":[[[77.0876,28.4595],[77.0890,28.4595],[77.0890,28.4610],[77.0876,28.4610],[77.0876,28.4595]]]}'::jsonb
);

insert into kraftshala.cohorts (id, name, campus_id, start_date, end_date) values (
  '00000000-0000-0000-0000-000000000001',
  'PGP AI-LED Marketing Batch 1',
  '00000000-0000-0000-0000-000000000001',
  '2026-06-01',
  '2027-03-01'
);
