# Kraftshala APM, Submission

## Part 1: Talk to One Real Human

### Who I spoke to

- **First name:** Manvir
- **Program:** IELTS coaching + Computer Science / Data Science coaching (an offline coaching center)
- **City:** Chandigarh
- **Duration:** A multi-month program, classes on most weekdays
- **Context:** Manvir is a Computer Science graduate from Thapar University. CGPA was around 6.5. After college, he job-hunted for roles aligned with where he wants to be in 5-7 years, did not find a fit, and decided to upskill in data science and move to Australia, where his elder sister and a large part of his extended family already live. His family also runs a ceramics business in Chandigarh that he helps with daily.

### What I chose to ask and why

I went in with a seven-question arc that moved from past to future, deliberately not jumping straight to attendance. The reason: I did not want him performing on the topic of my research. I wanted to first understand the shape of his life, then let the operational pain points surface naturally.

The arc I picked:
1. **Past.** What were you doing before this coaching, and how did you end up here?
2. **Why offline specifically.** Why a physical coaching center in 2026 when content is free on YouTube?
3. **Daily reality.** Walk me through a typical Tuesday.
4. **Confusion probe.** A direct follow-up on a contradiction I heard in his earlier answers (more below).
5. **Family stake.** How does your family read this decision? What is the safety net?
6. **Future.** Three to five years out, where do you see yourself?
7. **One thing to change.** If you could redesign one operational thing about the center, what would it be?

Why this arc and not just "tell me about attendance":
- Attendance is one operational pain point among many. If I had led with it, I would have anchored the entire conversation on a question that Kraftshala already thinks is important, and learned nothing I did not already know.
- I wanted to let him define what mattered to him. The thing he flagged at the end ("professionalism") was the single most useful thing I got from the call, and it would not have come up if I had only asked about attendance.

### The moment I was confused

In question 2 he said he chose offline specifically because self-study at home scatters his focus. He needed the physical-presence commitment. Then, two questions later, when I asked about operational breakdowns at the center, he said "nobody cares about attendance, log proxies bhi laga lete hain, fine hai" and added that he himself signs the attendance sheet when he is there.

These two statements collided for me. He bought offline for the accountability of showing up. But he treats the formal record of attendance as meaningless. I did not understand how both could be true.

What I did with the confusion: I stopped, named it explicitly, and asked him to reconcile. "Help me understand the gap. Physical presence matters to you, but the institutional record of it doesn't matter?"

His answer untangled it cleanly. **Attendance as a metric** (the sheet, the count, what the institution uses to decide whether he was there) is theatre to him, because anyone who wants to game it can. **Attendance as a personal commitment** (whether he actually walked in and sat in the room) is something he holds himself to. They are not the same thing. The sheet does not measure the commitment.

I did not get this distinction at first. Naming the confusion and asking the question got me there.

### What this told me about Kraftshala's offline experience that I did not know before

Three specific things I did not know before this call:

**1. Adult learners view classroom attendance as compliance theatre, not as a meaningful signal.** Manvir was direct: the paper sheet tells the institution nothing reliable, and the people for whom it should matter (those at risk of dropping off) are the same people most likely to fake it. Anyone who wants to game it, games it. This has a direct implication for Kraftshala: if attendance becomes a punishment or a public metric in the LMS, learners will resent it. If it stays a quiet, defensible record, they will tolerate it. The Part 2 design (cryptographic, silent, no signal cards visible) accidentally lands in the right place because of this exact insight.

**2. The thing adult learners actually buy from offline coaching is not content, it is accountability and focus.** Manvir explicitly reframed my question: "It's not about which coaching I'm taking, because in this AI-driven age knowledge is everywhere. The point is how efficiently I can absorb that knowledge." Kraftshala is selling a credential and a placement, but the daily product the learner is consuming is *commitment infrastructure*. Anything that makes the room feel less anchoring (a casual instructor, a missing checklist, a cancelled session communicated badly) directly damages what the learner paid for.

**3. The single most valuable operational lever is professionalism, not features.** When I asked him what he would change about the center's day-to-day, he did not say better attendance, better recordings, better WiFi, or a parent portal. He said professionalism. Specifically: the informality of how instructors and ops staff carry themselves felt regressive to him after Thapar. He framed it through a business operations lens (his family runs a business; he sees it that way too). For Kraftshala this is huge. Their offline batch will be judged less on the tech stack and more on whether the room *feels* like a serious institution. Punctual instructors, consistent communication, a clean classroom, ops staff who do not improvise. That is the actual differentiator, and it costs almost nothing in engineering.

The one-line takeaway for Part 2/3: **build attendance silently, sell professionalism loudly.**

---

## Part 2: Attendance, Designed and Defended

### The problem

Kraftshala's first offline batch starts in 6 weeks. 30 learners, one classroom in Gurugram, 9 months long. Until now attendance was just a Zoom join event. In the physical world, attendance does not exist yet. We have to build it from scratch and we have to build it for three different people who care about three different things.

### Why this matters, for each group

**For learners.** They are paying for a real outcome: a credential, a placement, a marketing career. Attendance is the official record that they showed up and did the work. If it gets messed up or feels punitive, they lose trust in the program quickly. They want marking to take 2 seconds, no tracking outside class, no face scans, and no need to file a ticket every time something goes wrong.

**For instructors.** They are the most expensive person in the room. Every minute they spend taking roll is a minute they are not teaching. They want a list that fills itself as the class fills, a quick review at the end for the edge cases, and a clear name to call out when something looks off.

**For Kraftshala.** Attendance is the first official record the institution will have to defend. It needs to be hard to fake, follow India's data protection rules (DPDP Act 2023), and be solid enough to survive a placement dispute. It also has to work just as well when we go from 30 learners in one room to 50 campuses, without us having to rebuild it.

### The solution, in three stages

#### Stage 1, the design (no constraints)

The system uses three checks. No single check is enough on its own. All three together are what make the record trustworthy.

**Check 1, is the right phone in the room?**
Each learner has one phone enrolled with Kraftshala. That phone holds a secret key that never leaves it. When the learner taps "Mark me present", the app uses that secret key to sign a short message and sends it to our server. The server checks the signature. If someone tries to mark you present from a different phone, the signature will not match and we reject it. In short: only your phone can mark you present.

**Check 2, is the phone actually in the classroom?**
Two things run silently in the background:
- **Bluetooth.** The instructor's phone acts as a beacon during class. It broadcasts a fresh code every session. Learner phones listen for that code. If they pick it up, they are close to the instructor.
- **Location.** The learner's GPS has to be inside the campus boundary.

Both checks must pass before the "Mark me present" button does anything. The screen never tells the learner which check passed or failed. It just enables or disables the button. If we showed the internal checks, we would basically be handing learners a how-to-cheat guide.

**Check 3, did a human actually see the learner?**
At the end of every session, the instructor sees a short list of names that the system has flagged (no Bluetooth signal, came in very late, suspicious pattern, etc.). The instructor calls out only those names. If the person answers, the instructor confirms them as present. If not, the instructor marks them absent with a quick reason. The instructor never has to recognise 30 faces. The system tells them exactly which names to call.

**Every learner ends up in one of four states:**
- **Present.** Phone in the room, instructor confirmed.
- **Absent.** No phone signal, instructor did not see them.
- **Present but not at lecture.** Phone was in the room but the person was not (someone left their phone behind to fake it).
- **Present but left in between.** Was there at the start, signal dropped during class.

**What the learner does, end to end.**
Walk in, open app, tap "Mark me present", see a confirmation screen with the time. Three taps, two seconds.

**What the instructor does, end to end.**
Open app, tap "Start session", roster fills in by itself as learners walk in, review the small flagged list at the end, tap "End session". Everything else is automatic.

**What we chose not to build.**
- No face recognition or selfie check. Easy to fake, invasive, and the instructor's own eyes work better.
- No new hardware in the classroom. The instructor's phone is the beacon. Zero setup, zero maintenance.
- No internet dependency at mark time. If the campus WiFi dies, attendance still works.
- No AI making decisions in version 1. Every flag has a clear rule behind it. If a learner disputes it, we can explain exactly why.

#### Stage 2, the mockup

Designed in Stitch using the Daakwala design system (clean, Revolut-style look: off-white background, bold black headlines, one royal-blue accent for the main button). 13 screens cover the full flow.

Onboarding (shared by learner and instructor): role selection, login by magic link, permissions, terms and conditions. A shared Bluetooth-off screen handles the case when the user has BT switched off.

Learner side: home (today's lectures), lecture detail with the "Mark me present" button (no signal cards visible), confirmation screen, sessions history, past session detail.

Instructor side: home (action-first card with "Start session"), live cockpit (roster, filter chips for Present / Absent / Not in room / Left early, per-row override sheet, "End session" button), sessions history, past session detail.

**Links:**
- Stitch mockups: https://stitch.withgoogle.com/project/6080344267273329802
- GitHub repo (full code, schema, edge functions, web dashboard scaffold): https://github.com/Ashwini7506/kraftshala
- Android APK (debug build, install on any Android 8+ device): https://github.com/Ashwini7506/kraftshala/raw/main/dist/kraftshala-debug.apk

Built and shipped as a working Android app on a real device, with the backend wired end-to-end. The full mark-me-present flow saves a real signed record to the database, and the instructor's cockpit shows it live.

#### Stage 3, defending against reality

**Scenario 1. Four learners walk in 20 minutes late, no coordinator in the room.**

They open the app and tap "Mark me present". The instructor's phone is still broadcasting Bluetooth. Their GPS is inside campus. Their phone signs the request. The system marks them present, with the actual time stamp (11:20 AM). At the end of the session, the instructor sees their names in the Present list but with a "Marked 11:20" stamp visible. No action needed.

If Kraftshala's policy says more than 15 minutes late is absent, the dashboard automatically flips them to "Present but not at lecture" overnight. The coordinator does not need to be in the room for any of this. The system handles late arrivals as data, not as exceptions.

**Scenario 2. Campus WiFi goes down 5 minutes before the session.**

The attendance system does not need WiFi to capture marks.
- Bluetooth is phone-to-phone, no WiFi.
- GPS works offline.
- The phone signs the mark locally.
- The mark gets saved to the phone's local storage with a "pending" tag.
- The instructor's live roster falls back to local-only and resyncs when WiFi returns.

When WiFi comes back, every pending mark uploads with its original timestamp intact. Nobody loses their attendance.

Worst case (WiFi down AND instructor's phone dies): printed paper roster at the door, coordinator enters the data next morning. The system logs that as a "manual entry" so we know how often we are falling back to paper.

### What we ship and when

**Version 1 (in 6 weeks, before Day 1).** Database, login, role-based home, Bluetooth broadcasting and scanning, signed mark + location check, live roster, end-of-session review, web dashboard (sessions list, flagged learners, history), offline-first behaviour with sync on reconnect, QR flow for lost phones.

**Version 1.1 (3 weeks after Day 1).** Bluetooth mesh relay (so larger rooms can extend the instructor's signal through learner phones), an iOS version of the app, random in-session pings to catch proxy attempts.

**Version 1.5 (3 months in).** Pattern detection on the flagged buckets, support for multiple campuses, deeper integration with the LMS.

**Version 2 (6 months in).** AI-powered anomaly detection, natural-language search on the dashboard, predictive at-risk flagging.

### Success metrics

| Metric | Target, 30 days after launch |
|---|---|
| Learners enrolled with a device | 100% (30 out of 30) |
| Sessions where all three checks worked | 95% or higher |
| Manual overrides per session | Fewer than 2, trending to zero |
| "Present but not at lecture" flags per week | Fewer than 5, trending to zero |
| Offline marks that synced successfully | 99% or higher |
| Time to resolve a dispute | Less than 24 hours |
| Sessions that worked without manual help | 95% or higher |

### Tradeoffs

1. **One device per learner.** Adds friction when someone upgrades their phone. Solved with a QR re-pairing flow (one-time code, 10-min window, capped at one re-pair per week). More than one in a week and the system auto-flags it.
2. **Instructor's phone as the Bluetooth beacon, not a separate device.** Zero cost, zero setup per classroom. We lose backup if the instructor's phone dies mid-session. Paper roster covers that.
3. **No face recognition.** We give up one signal of human identity. But the instructor calling out names at the end is cheaper, more accurate, and not creepy.
4. **The learner does not see the internal checks.** We lose a bit of debug-ability for the user. We gain a smaller attack surface. A learner cannot see exactly what to fake.
5. **No AI in version 1's main flow.** We lose pattern-detection sophistication. We gain zero false positives and a clear audit trail. No "the algorithm flagged me" disputes.
6. **One Bluetooth beacon for version 1.** Works fine for a 30-learner room. Bigger rooms (over 15m wide) will need the mesh relay, which is in version 1.1.
7. **Paper roster as the last fallback.** Inelegant. But it is the only thing that works when every screen on campus is dead.
8. **Learners without a smartphone.** Some learners may only have a keypad phone. The system cannot reach them. Fallback: coordinator marks them manually from the dashboard, tagged as "manual mark". Long-term, Kraftshala can include a basic Android phone in the onboarding kit if this becomes common.
9. **A learner with two phones.** If a learner enrols phone A and then leaves it in the classroom while going home with phone B, the system cannot tell. Three things mitigate this. First, only one device per learner is active at a time (the QR re-pairing flow revokes the old one). Second, the instructor's name-callout catches the person not being in the room. Third, pattern detection in version 1.5 will flag learners who mark in but never actually get called present. We accept this as residual risk for version 1.

### What is already built today

- Full Android app (Compose, Kotlin) with all 13 screens, role-based navigation, login by magic link with deep-link return into the app.
- Backend on Supabase: 9 tables in a dedicated `kraftshala` schema, security rules, helper functions, 4 edge functions for signature verification and session management.
- Live updates wired in: when a learner marks in, the instructor's cockpit reflects it immediately without refresh.
- A demo-mode flag for debug builds so the full end-to-end flow runs without deploying the signature-verification function.
- 30-student test cohort seeded. End-to-end test passed: a real learner signed in by magic link, tapped "Mark me present", the record landed in the database with a valid signed token, and the instructor's cockpit showed it live with the correct name and timestamp.

---

## Part 3: Prioritise Under Constraint

The setup. One PM (me). Two engineers, both already committed to other work. Six weeks to Day 1. Realistically, each person has around 25 effective engineering days. That is enough for one big build, one small fix, and one architectural decision. Anything else has to be solved with people and process.

The test I apply to every item: **will the first session on Day 1 fail or feel broken if this is not fixed?** If yes, it makes the cut. If no, defer it or hack a manual workaround until the cohort tells me to invest more.

### MoSCoW view of the eight problems

| Bucket | Problems | The logic |
|---|---|---|
| **Must-have** | #1 Zoom link, #2 Attendance, #5 Classroom tech, #6 Internet, #7 Parents | Day 1 fails or feels broken without this. |
| **Should-have** | #3 Recordings, #8 Missed-session policy | A manual or written workaround is OK on Day 1. Harden later when we have real cohort behaviour data. |
| **Won't-have (batch 1)** | #4 Online learners attending offline | Billing, capacity, and equity all get messy. Defer to batch 2. |

Five Musts but only three engineering slots. So Step 1 picks the three Musts that need code (#2, #1, #6). The remaining two Musts (#5, #7), both Shoulds (#3, #8), and the Won't (#4) become Step 2: five items handled with people and process before Day 1. Step 3 is the one of those five I am most worried about leaving unsolved.

### Step 1, the top 3 (the engineering bets)

**Priority 1: Problem #2, Attendance for the physical classroom**
- **Why first.** Day 1 has no data without this. Kraftshala's placement promise is partly tied to attendance, so disputes will eventually land in the office and we need a record that holds up.
- **How we fix it.** Ship the Part 2 design: signed mark from the learner's phone, Bluetooth check against the instructor's phone, GPS check against the campus, offline-first capture, instructor review at the end. Already built and demoable.
- **Two-week success signal.** Over 95% of session marks come in clean from all three checks. Fewer than 2 manual overrides per session.

**Priority 2: Problem #1, Remove the Zoom link from the offline LMS view**
- **Why second.** A learner sitting in Room 204 sees a "Join Zoom" button on the LMS, taps it (and looks ridiculous), DMs a coordinator (creating support load), or worst, walks out thinking they are in the wrong place. Tiny engineering effort, huge daily trust impact.
- **How we fix it.** Add a session type field (online / offline / hybrid) to the LMS session record. Show the Zoom link only for online. Show the room number for offline. Two days of engineering, one day of testing.
- **Two-week success signal.** Zero "what's this Zoom link" tickets from offline learners. Zero offline learners accidentally on a dead Zoom during a live session.

**Priority 3: Problem #6, Make offline-first a hard rule for everything we build**
- **Why third.** Not a feature, a design rule. The campus WiFi will go down. If anything we ship depends on the cloud at the exact moment someone is using it, the room breaks. Solving this once makes #2 and every future tool resilient by default.
- **How we fix it.** Every learner action writes to their phone's local storage first with a "pending" tag, then queues and uploads when the connection returns (with the original time stamp). SMS shortcode (`PRESENT <code>` to a Gupshup number) is the second fallback at about 20 paise per message. The coordinator's 4G hotspot is the third. Paper is the floor.
- **Two-week success signal.** At least one WiFi outage during a live session is absorbed cleanly. Not a single learner unable to mark in. Zero coordinator escalations during the outage.

### Step 2, the other 5 (handled before Day 1 without engineering)

**Problem #5, No classroom tech standard**
- **Why it matters.** The room is the product on Day 1. A missing HDMI adapter or a projector that does not turn on breaks the first impression of a 9-month program.
- **How we handle it.** Week 1, PM walks the room with the venue manager and photographs everything. Week 2, write a one-page must-have checklist: 3000-lumen projector or 75-inch panel, ceiling speakers with a 30W amp, a wireless lapel mic plus a handheld for Q&A, HDMI / USB-C / VGA adapters at the lectern, six power points around the presenter plus one per learner pair, WiFi that handles 35 devices, a wired Ethernet drop as fallback, a 6x4 whiteboard plus markers and eraser. Ops procures gaps by week 4.

**Problem #7, Parents calling the office**
- **Why it matters.** Even though learners are adults, Kraftshala's own alumni testimonials say parental concern is a real enrolment barrier. If the office team gives inconsistent answers on Day 1, the batch's reputation cracks before week 2.
- **How we handle it.** A Day-0 parent letter (English and Hindi) on enrolment confirmation. A named parent helpline staffed 9am to 8pm Mon to Sat by one ops associate (modelled on Scaler SST's 7-day Customer Support setup). One on-campus parent orientation the weekend before Day 1 (90 minutes covers about 80% of pre-launch anxiety). A parents-only WhatsApp broadcast list (not a group, no parent-to-parent noise) for timing changes and safety updates. A public parent FAQ on the website. PM writes the letter and the FAQ in two days.

**Problem #3, Session recordings**
- **Why it matters.** Online cohort is paying for recordings of offline classes too. The missed-session policy depends on whether a recording exists. But we do not yet know what recording quality the cohort actually wants, so building a pipeline now risks shipping the wrong thing.
- **How we handle it.** Manual on Day 1. One coordinator films from a tripod-mounted phone, uploads to Drive within two hours of class, drops the link into the LMS session entry. Gives us 4 to 6 weeks of real re-watch data. If re-watch rate crosses 40% by week 6, greenlight an automated setup (OBSBOT Tail Air + OBS auto-upload, around 85k INR). Until then, a premature pipeline burns engineering capacity for an unclear demand signal.

**Problem #8, No missed-session policy**
- **Why it matters.** The first time someone misses a session and asks "what now?", the absence of a written answer breaks trust. Especially when learners can compare with the online cohort that has recordings.
- **How we handle it.** PM writes a one-page policy in week 3, leadership signs off in week 4, ships inside the learner onboarding email before Day 1. Defines excused (medical, family emergency, prior written intimation) vs unexcused, how to catch up (peer notes, instructor office hours, project submission), and how attendance affects placement readiness. The product version (a catch-up view tied to attendance status) builds on #3 later.

**Problem #4, Online cohort wanting to attend offline sessions**
- **Why it matters.** Online learners pay a different fee and the room only seats 30. Letting them shuffle in and out makes Day 1 operations noisier and creates an equity issue with the offline cohort that actually paid for that seat.
- **How we handle it.** Deferred to batch 2 as a deliberate cut. For batch 1, a Google Form takes ad-hoc visit requests, capped at five guests per session, coordinator approves the day before. The deferral is communicated up front so nobody feels ambushed. The actual message is Step 3.

### Step 3, the message I am most worried about leaving unsolved

The one I keep thinking about is **#4, the online cohort asking to attend offline.** Parents I can handle with a letter and a helpline. Missed-session policy is a one-page doc. Internet is an architecture decision. Recordings have a manual workaround. But online learners are paying customers who want a thing I am not going to give them in batch 1. How that gets communicated decides whether they feel respected or fobbed off. Get the tone wrong, and the same people will write the Reddit post that costs us batch 3 enrolments.

**Channel.** Direct WhatsApp message from the program lead (not an ops associate, not a generic broadcast) to each online-cohort learner who has already raised the request. Email as a follow-up, for the record. Sent on the same day we announce the offline batch dates internally, so they hear it from us before they hear it from anyone else.

**Subject line / WhatsApp opener:**
About your request to sit in on the offline batch

**Message body.**

Hi [first name],

Thanks for asking about joining the offline sessions in Gurugram. I am writing because I owe you a straight answer instead of an ops template.

For batch 1, we cannot take you in regularly. The reasons are honest and not glamorous. The room seats 30, we have admitted 30, and we do not want to turn the first cohort into a place where seats float around. Day 1 has a lot of operational firsts for us (attendance, AV setup, parent comms, classroom flow), and the cleanest way to ship those is to keep the room boundary tight. If we shuffle people in and out, we cannot honestly measure what is working.

What I can offer instead, starting from week three:
1. A guest-visit slot. One ad-hoc visit per online learner per quarter, requested at least 48 hours in advance through a short form we will share next week. Capped at five guests per session, prioritised by topic fit.
2. Every offline session recorded and posted to your LMS within 24 hours. Same content, same instructor, same Q&A captured.
3. A monthly "Offline AMA" on Zoom, where the offline-batch instructor takes questions from online learners on what is being taught and how it differs from the recorded version.

Batch 2 is where the offline experience opens up more seriously. We will publish the framework for cross-cohort access (criteria, fee difference, calendar) by the end of batch 1, in time for batch 2 enrolments.

If this answer does not work for you, please tell me. I would rather hear it now and find a path than have you walk away quietly. My number is on this thread, or write to me at [program-lead@kraftshala.com].

[Program Lead Name]
Program Lead, PGP in AI-Led Marketing
