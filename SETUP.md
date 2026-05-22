# Setup — Kraftshala Attendance System

End-to-end deployment from zero to working demo.

---

## 1. Supabase backend

### Create the project

1. Sign up at [supabase.com](https://supabase.com) (free tier is enough).
2. Create a new project named `kraftshala-attendance`.
3. From the project settings, copy:
   - **Project URL** (`https://<project-ref>.supabase.co`)
   - **anon public key**
   - **service_role secret key** (keep this safe; used only by edge functions and admin scripts)

### Apply the schema

```bash
cd kraftshala
npm install -g supabase
supabase login
supabase link --project-ref <YOUR_PROJECT_REF>
supabase db push   # applies supabase/migrations/001_initial_schema.sql
```

### Deploy the edge functions

```bash
cd supabase/functions
supabase functions deploy generate-nonce --no-verify-jwt
supabase functions deploy verify-signature --no-verify-jwt
supabase functions deploy qr-pairing
supabase functions deploy finalise-session
```

Set the service-role secret for the functions:

```bash
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<your_service_role_key>
```

### Create test users (run in the Supabase SQL editor)

```sql
-- After a user signs up via Auth, you'll need to insert their profile row.
-- For testing, create users via the dashboard then insert into our `users` table.
-- Replace <uuid> with the actual auth.users.id values from the dashboard.

insert into users (id, full_name, email, role, cohort_id) values
  ('<student_uuid>', 'Sumit Shah', 'sumit@example.com', 'student',
   '00000000-0000-0000-0000-000000000001'),
  ('<instructor_uuid>', 'Priya Mehra', 'priya@kraftshala.com', 'instructor', null),
  ('<admin_uuid>', 'Admin User', 'admin@kraftshala.com', 'admin', null);
```

---

## 2. Web dashboard (Next.js)

```bash
cd web
npm install
cp .env.example .env.local
# Edit .env.local:
#   NEXT_PUBLIC_SUPABASE_URL=<your project url>
#   NEXT_PUBLIC_SUPABASE_ANON_KEY=<anon key>
#   SUPABASE_SERVICE_ROLE_KEY=<service role key>

npm run dev   # http://localhost:3000
```

Sign in at `http://localhost:3000/login` with the admin email you seeded.

### Deploy to Vercel

```bash
cd web
npm install -g vercel
vercel login
vercel --prod
```

Add the three env vars in Vercel project settings. The dashboard auto-deploys on push to `main`.

---

## 3. Android app

### Local properties

In `android/local.properties`, set:

```
sdk.dir=/Users/<you>/Library/Android/sdk
supabase.url=https://<project>.supabase.co
supabase.anonKey=<anon key>
```

### Build and install

1. Open `android/` in Android Studio (Hedgehog or later).
2. Let Gradle sync.
3. Plug in a real Android 12+ phone (BLE needs real hardware, not emulator).
4. Build → Run.

Repeat for two devices: one logged in as instructor, one as student.

### Permissions

On first launch, grant:

- Bluetooth (Scan, Connect, Advertise)
- Location (Fine + Background)
- Notifications

These are required for BLE + geofencing.

---

## 4. Verify end-to-end

1. **Sign in** both phones via magic link (check both inboxes).
2. **Instructor phone**: from the home screen, start a scheduled session. The foreground service notification appears.
3. **Student phone**: walk into BLE range. The "Mark me present" button enables.
4. Tap mark. Within 3 seconds, the instructor's roster updates.
5. **Web dashboard**: open `/dashboard/sessions`, see the active session and live attendance.
6. On the instructor phone, tap **Close session**. Final flags lock, audit log writes.
7. On the dashboard, open `/dashboard/anomalies` and `/dashboard/audit` to verify the records.

---

## 5. Demo recording for Kraftshala submission

```bash
# On your Mac, use QuickTime Player to record both phone screens via USB.
# Capture: onboarding → start session → mark in → close → web dashboard view
# 3-5 minutes max.
```

Submit:
- This GitHub repo link
- The signed-release APK (`android/app/build/outputs/apk/release/app-release.apk`)
- The Vercel dashboard URL
- The screen recording video
- `architecture.md` + `requirements.md` + `plan.md` as docs

---

## 6. Common errors

- **"BLE advertising not supported"** — phone hardware doesn't support BLE peripheral mode. Try a Pixel or Samsung mid-range device.
- **"Foreground service permission denied"** on Android 14 — make sure `FOREGROUND_SERVICE_CONNECTED_DEVICE` and `FOREGROUND_SERVICE_LOCATION` are granted at runtime.
- **Edge function 401** — verify your `SUPABASE_SERVICE_ROLE_KEY` is set via `supabase secrets set`.
- **Realtime not updating** — ensure RLS policies allow the user's role to SELECT the rows; subscription requires SELECT permission.
