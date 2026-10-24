# CholoGO

## Table of Contents

- [Project Description](#-project-description)
- [Project Features](#-project-features)
- [Objectives](#-objectives)
- [Target Audience](#-target-audience)
- [Project Structure](#-project-structure)
- [Firebase Collections & Services](#-firebase-collections--services)
- [Backend Server (Render)](#-backend-server-render)
- [Website & Web Dashboard](#-website--web-dashboard)
- [Milestones](#-milestones)
- [Technologies Used](#-technologies-used)
- [Installation](#-installation)
- [Team Members](#-team-member)
- [Live Project](#️-live-project)
- [Future Improvements](#-future-improvements)

---

## 📝 Project Description

**CholoGO** is a ride-sharing app for university students. It gives verified students a safe, affordable, closed community where they can offer or request rides.

It starts with **Ahsanullah University of Science and Technology (AUST)**. Riders are students with a bike or a car, and passengers are students heading to campus or home. CholoGO supports **Ride Now** for instant matching and **Tomorrow Ride** for planning the next day's commute. Once matched, both people get a **live map**, an **in-ride chat**, and a confirmed start/finish flow.

The Android app is built with **Kotlin**, **Jetpack Compose**, and **Firebase**:

- **Firebase Authentication** (email/password and Google Sign-In) handles accounts.
- **Cloud Firestore** stores users, rides, requests, ride history, an XP ledger, ratings, reports, and chat messages.
- **Firebase Realtime Database** carries live rider/passenger locations during a trip.
- **MapLibre** renders a fully **offline map of Dhaka** from a bundled tiles file, with no map API key needed.
- A small **Node.js/Express server on Render** handles the few jobs that need admin privileges: cross-user cancellation, push notifications, and chat cleanup.
- A **Next.js website** on Netlify serves as the public marketing site and a web dashboard with the same Ride Now and Tomorrow flows.

---

## 💡 Project Features

### i. User Authentication and Role Selection

- Sign up and log in with email/password using Firebase Authentication.
- **Sign in with Google** through the Credential Manager API.
- Forgot-password flow.
- First-time Google users complete their profile (role and phone number), since Google provides neither.
- Pick a role after signing up: **Passenger** or **Rider**.
- **Vehicle selection for riders:** bike (always one pillion seat) or car (choose 1–4 seats per trip), plus model, plate number, and color.
- **Guest browsing:** look around the app before signing in. A banner invites guests to sign in when an action needs an account.
- Separate dashboards for passengers and riders, and a profile page with stats.
- Role-based access enforced by Firestore security rules.

### ii. App Layout and Theme

- Bottom navigation with three tabs: **Ride Now**, **Map**, and **Tomorrow**.
- **Light/dark theme toggle**, remembered between launches.
- Time-aware greeting on the dashboard.
- Layout tuned for both phones and tablets.
- Local ad/sponsor banner slot.

### iii. Passenger Features

- Request an instant Ride Now ride and get matched with a live rider.
- Plan Tomorrow rides (to campus and/or to home).
- See the matched rider's name, phone number, and **vehicle details** (type, model, plate, color).
- Call the matched rider directly.
- **Chat with the matched rider** in the app.
- **See the rider's position on a live map** while the trip is active.
- Confirm trip start and safe arrival, for both Ride Now and Tomorrow.
- Rate the rider or report an issue after a completed trip.
- Push notification when a rider accepts a Tomorrow request, and a notice with a one-tap resubmit if the rider later cancels.
- Can cancel an accepted Tomorrow ride. The server restores the rider's seat immediately.

### iv. Rider Features

- Go live for Ride Now requests with pickup, destination, time, and seats.
- Save Tomorrow rides for passengers to match against.
- View incoming requests, including the passenger's phone number once matched.
- Accept or decline requests.
- Start and complete trips, with passenger confirmation at each step.
- **Chat with the passenger** and **share live location** on the trip map.
- **Rate passengers** after a completed trip (two-way rating).
- Way out of trips an unresponsive passenger left open: riders can force-close a stuck Ride Now trip after a grace period.
- Goes offline automatically after a Ride Now trip completes or is cancelled.
- Earn XP for completed trips and track level, title, and progress.
- Push notification the moment a passenger's request matches a saved Tomorrow ride.

### v. Ride Now System

- Real-time matching between passengers and live riders.
- A passenger can have only one active Ride Now request at a time.
- Matching uses pickup, destination, route key, time window, available seats, active rider status, and request expiry.
- Abandoned requests are swept, so a passenger is never stuck on an old trip card.
- Status flow:
  `SEARCHING → NOTIFIED → ACCEPTED → START_PENDING_CONFIRMATION → ONGOING → END_PENDING_CONFIRMATION → COMPLETED`
  plus `CANCELLED`, `EXPIRED`, and `ISSUE_REPORTED`.

### vi. Tomorrow Ride System

- Riders save rides for the next day, and passengers request them.
- Matching uses ride date, pickup, destination, direction, time range, and seat availability.
- Same full trip lifecycle as Ride Now:
  `PENDING → ACCEPTED → START_PENDING_CONFIRMATION → ONGOING → END_PENDING_CONFIRMATION → COMPLETED`, plus `CANCELLED`.
- The Start Trip button appears only close to the scheduled time.
- A cancelled request can be resubmitted, returning it to `PENDING` for other riders.
- **Missed-ride reconciliation:** if a matched trip was never marked finished, both sides answer "did this ride happen?" in Ride History.
  - Matching answers complete or close the trip.
  - Conflicting answers mark it unverified.
- **On-device reminder** an hour before a confirmed Tomorrow ride, asking whether it's still happening.

### vii. Live Trip Map 🗺️

- **Offline map of Dhaka:** streets, buildings, and water come from a Dhaka-only `.pmtiles` file bundled in the app and drawn with MapLibre. No API key or internet is needed for the base map, and there are separate light and dark map styles.
- The **Map tab** shows the AUST campus. Tapping the map gets a fresh GPS fix and shows "You are here".
- During a matched trip:
  - The rider shares their position about every 10 seconds.
  - The passenger shares their position once.
  - Both appear as colored pins (rider in blue, passenger in green), and the camera frames both.
- The full-screen map **opens automatically once** when a trip is accepted, and you can reopen it from **View live map**.
- Shared locations are cleared the moment the trip leaves its active statuses.

### viii. In-Ride Chat 💬

- Short-lived chat between a passenger and their matched rider, for both Ride Now and Tomorrow trips.
- Unread badge, quick replies ("I'm here", "On my way", "2 min away", …), and read-only mode after the trip ends.
- Push notification to the other person, throttled so a burst of messages doesn't flood them. No push is sent if that chat is already open on screen.
- Only the matched pair can read the thread. Messages can be posted only while the trip is open, and nobody can edit or delete them.
- **Each thread is deleted automatically 24 hours after its trip ends.**

### ix. Ratings, Reports, and Ride History

- Two-way star ratings after a completed trip (passenger rates rider, rider rates passenger), for both Ride Now and Tomorrow.
- Issue reports, with a report count on the user's profile for moderation.
- The security rules require evidence that the two users actually shared a trip before a rating, report, or completed-ride count is accepted.
- You can rate and report only once per trip. The buttons disable afterwards.
- **Ride History** lists completed Ride Now and Tomorrow trips for both roles and hosts the missed-ride review.

### x. Gamification (Evidence-Backed XP)

- XP is the **sum of entries in an append-only `xp_events` ledger**, not a counter users can edit. Each entry:
  - has an ID derived from the trip,
  - has an amount fixed by the security rules, not the app,
  - points to a document proving the trip happened.
- Current rewards:
  - **Rider:** +12 XP per completed trip.
  - **Passenger:** +8 XP per completed trip.
  - **Planning a day's travel:** +2 XP, once per travel date.
- Nothing pays out before a trip completes, so accept/cancel loops earn nothing.
- Level system with 11 titles: Beginner, Explorer, Regular, Trusted, Advanced, Expert, Elite, Master, Champion, Legend, and Ultimate. Shows current XP, level, and progress to the next level.

### xi. Push Notifications & Reminders

- **Firebase Cloud Messaging** reaches the device even when the app is closed. Notifications are sent for:
  - Tomorrow request accepted (to the passenger).
  - New Tomorrow match (to the rider).
  - New chat message (to the other person on the trip).
- Local notifications:
  - Rider cancelled an accepted Tomorrow ride.
  - One-hour reminder before a Tomorrow ride (WorkManager).
- Notification permission prompt on Android 13+.

### xii. Closed Community

- Built for university students, starting with AUST.
- Can later expand to NSU, BRAC University, SEU, UIU, ULAB, and others.
- Limiting users to verified student communities makes ride-sharing safer.

---

## 🎯 Objectives

- **Student-only platform:** ride-sharing limited to university students.
- **Lower travel costs:** a cheaper alternative to commercial ride-sharing.
- **Better campus commuting:** easier trips to and from campus.
- **Community trust:** a closed community, two-way ratings, and evidence-backed reputation.
- **Instant and planned rides:** Ride Now and Tomorrow Ride.
- **Coordination during the trip:** live map and in-ride chat, so the two people find each other without endless phone calls.
- **Rider motivation:** XP and levels that can't be farmed.
- **Real-time updates:** push notifications, so nobody has to keep the app open.
- **Room to grow:** an MVP that can expand to other universities and to the web.

---

## 👥 Target Audience

- AUST students who need affordable rides.
- Student bikers and car owners who want to share their commute.
- University students who travel regularly between home and campus.
- Students looking for safer, community-based ride-sharing.
- Other university communities in Dhaka in the future.

---

## 🗂 Project Structure

```
choloGO/
├── app/                    # Android app (Kotlin + Jetpack Compose)
│   └── src/main/
│       ├── assets/         # dhaka.pmtiles + light/dark map styles (offline map)
│       └── java/com/example/chologo/
│           ├── data/       # models + Firestore/RTDB repositories
│           ├── viewmodel/  # Auth, RideNow, TomorrowRide, RideChat
│           ├── notifications/  # FCM service, reminders (WorkManager)
│           └── ui/         # auth, passenger, rider, map, chat, common, theme
├── server/                 # Node.js/Express REST API (deployed on Render)
├── web/                    # Next.js marketing site + web dashboard (Netlify)
├── functions/              # Legacy Firebase Cloud Function (superseded by server/)
├── firestore.rules         # Version-controlled Firestore security rules
└── firestore.indexes.json  # Composite indexes
```

---

## 📜 Firebase Collections & Services

> Firebase handles authentication, data, and security rules. The small standalone server ([Backend Server](#-backend-server-render)) covers the few operations the rules can't safely allow on their own.

### Authentication

- Email/password signup and login, plus forgot password.
- Google Sign-In (Credential Manager + Firebase `GoogleAuthProvider`).
- Session handling and navigation to the right dashboard for each role.

### `users`

```kotlin
{
    uid, name, email, phone: String,
    role: String,                 // passenger or rider
    vehicleType: String,          // bike or car
    vehicleModel, vehicleNumber, vehicleColor: String,
    university, studentId, homeLocation: String,
    xp: Long,                     // cached display value; the ledger is the source of truth
    ratingAverage: Double,
    ratingCount, reportCount, completedRideCount: Int,
    fcmTokens: List<String>,
    createdAt: Long
}
```

### `rides` (Tomorrow rides saved by riders)

```kotlin
{
    rideId, riderId, riderName: String,
    tripDirection: String,        // to_campus or to_home
    pickup, destination, tripTime, routeKey, rideDate: String,
    timeMinutes: Int,
    vehicleType, vehicleModel, vehicleNumber, vehicleColor: String,
    totalSeats, availableSeats: Int,
    status: String,               // active, full, cancelled
    isTomorrowSetup: Boolean,
    createdAt: Timestamp
}
```

### `ride_requests` (Tomorrow requests from passengers)

```kotlin
{
    requestId, userId, passengerName, passengerPhone: String,
    pickup, destination, tripDirection, tripTime, routeKey, rideDate: String,
    hour, minute, timeMinutes: Int,
    status: String,               // pending, accepted, start_pending_confirmation, ongoing,
                                  // end_pending_confirmation, completed, cancelled
    matchedRideId, matchedRiderId, matchedRiderName, matchedRiderPhone, matchedRideTime: String,
    matchedVehicleType, matchedVehicleModel, matchedVehicleNumber, matchedVehicleColor: String,
    rideStartedByRider, rideConfirmedByPassenger, rideEndedByRider, rideCompletedByPassenger: Boolean,
    riderHappenedAnswer, passengerHappenedAnswer,   // missed-ride reconciliation
    riderRated, passengerRated, issueReported: Boolean,
    rating, passengerRating: Int,
    reportReason, reportDetails: String,
    rejectedByRiderIds: List<String>,
    cancelledBy, cancelledByRole, cancellationReason: String,
    acceptedAt, startedAt, completedAt, cancelledAt, createdAt: Timestamp
}
// subcollection: messages/{messageId} — in-ride chat
```

### `live_rides` (riders who are live for Ride Now)

```kotlin
{
    rideId, riderId, riderName: String,
    pickup, destination, tripDirection, tripTime, routeKey: String,
    timeMinutes, availableSeats: Int,
    vehicleType, vehicleModel, vehicleNumber, vehicleColor: String,
    status: String,               // active, inactive
    isLiveNow, isAvailable: Boolean,
    currentRequestId: String,
    createdAt, lastUpdatedAt: Timestamp
}
```

### `ride_now_requests`

```kotlin
{
    requestId, passengerId, passengerName, passengerPhone: String,
    pickup, destination, tripTime, routeKey: String,
    timeMinutes: Int,
    status: String,               // searching, notified, accepted, start_pending_confirmation, ongoing,
                                  // end_pending_confirmation, completed, cancelled, expired, issue_reported
    matchedRideId, matchedRiderId, matchedRiderName, matchedRiderPhone: String,
    matchedVehicleType, matchedVehicleModel, matchedVehicleNumber, matchedVehicleColor: String,
    statusEnteredAt, closedByRole, closedAt,         // force-close support
    riderRated, passengerRated, issueReported: Boolean,
    rating, passengerRating: Int,
    reportReason, reportDetails: String,
    acceptedAt, startedAt, completedAt, cancelledAt, expiredAt, createdAt, expiresAt: Timestamp
}
// subcollection: messages/{messageId} — in-ride chat
```

### Other collections

| Collection | Purpose |
|---|---|
| `ride_history` | One entry per completed trip (Ride Now or Tomorrow), read by both people on the trip in Ride History. |
| `xp_events` | Append-only XP ledger: `eventId, userId, reason, role, sourceId, amount, createdAt`. |
| `ride_ratings` | Star ratings (1–5) with optional comment, in either direction. |
| `ride_reports` | Issue reports for moderation. |
| `notifications` | Notices for a user, readable only by that user. The only change a user can make is marking one as read. |
| `suggestions` | Public suggestion box from the website. Anyone can submit (length-capped), and nobody can read submissions back except through the Firebase console. |
| `ride_chats` | Server-only chat metadata (push throttling, close time for the 24-hour cleanup). |

### Realtime Database: `rideNowLocations/{requestId}/{rider|passenger}`

Live `{ lat, lng, timestamp }` positions for the trip map, cleared when the trip ends.

### Firestore Security Rules

All rules are in [`firestore.rules`](firestore.rules) and kept in version control. Instead of one broad rule per collection, each status change (accept, start, confirm, complete, cancel, rate, report, reconcile) has its own narrow function. That function pins the exact previous status, the new status, and the exact fields allowed to change. Rating, report, and completed-ride counter updates must point to a trip the two users actually shared. XP entries must match the amounts and IDs the rules compute themselves.

---

## 🌐 Backend Server (Render)

Almost everything goes straight from the app to Firestore under the rules above. A small **Node.js + Express + TypeScript** server using the **Firebase Admin SDK** covers what a client can't be trusted to do:

- Cancelling an accepted Tomorrow request. This updates the passenger's request and the rider's seat count in one transaction.
- Sending **FCM push notifications**, which need Admin SDK credentials that should never ship in an app.
- Deleting in-ride chat threads 24 hours after their trip ends. A background sweep runs every 30 minutes.

**Hosting:** [Render](https://render.com) free tier, auto-deploying from `main`.
**Live URL:** https://chologo.onrender.com

> ⚠️ The free tier shuts the server down after 15 minutes without traffic, so the first request afterwards can take 30–60 seconds. The app's HTTP client uses long timeouts to allow for this.

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/tomorrow/cancel-request` | Cancels the caller's own accepted Tomorrow request and restores the rider's seat in one transaction. |
| `POST` | `/api/tomorrow/notify-accepted` | Called by the accepting rider. Sends the passenger a "request accepted" notification. |
| `POST` | `/api/tomorrow/notify-match` | Called by a passenger after submitting a request. Notifies any rider whose saved ride matches, without repeating notifications for the same pair. |
| `POST` | `/api/chat/notify` | Sends a chat-message notification to the other person on the trip, throttled per thread. |
| `GET` | `/health` | Health check. |

Every `/api/*` endpoint requires a Firebase ID token (`Authorization: Bearer <token>`). The server also checks independently that the caller owns the relevant side of the trip, so nobody can act as another user.

### Local development

```bash
cd server
npm install
npm run dev      # ts-node-dev, live reload
npm run build    # type-check + compile to dist/
npm start        # run the compiled server
```

The server needs a `FIREBASE_SERVICE_ACCOUNT` environment variable containing the JSON of a Firebase service account key. Keep it out of source control: set it in Render's environment settings, or in your own shell/`.env` when running locally.

---

## 🖥 Website & Web Dashboard

The `web/` folder is a **Next.js** app (React, Tailwind CSS) deployed on **Netlify** at https://chologoaust.netlify.app/. It uses the same Firebase project as the Android app.

- **Marketing pages:** home (problem, solution, and why it works), features, download, contact, privacy, and terms.
- **Public suggestion box** on the Contact page, with no login required.
- **Web dashboard** (email/password sign-in) that knows your role and offers:
  - live Ride Now and Tomorrow flows for passengers and riders,
  - ratings,
  - missed-ride review,
  - Ride History,
  - profile and XP level card.
- A server-side proxy route (`/api/tomorrow/[action]`) forwards to the Render server's Tomorrow endpoints.

```bash
cd web
npm install
npm run dev      # http://localhost:3000
```

---

## 📝 Milestones

### Milestone 1: Initial Setup and Authentication ✅
Android Studio project, Kotlin + Compose, Firebase, signup/login/forgot password, role selection, and role-based navigation.

### Milestone 2: Passenger and Rider Dashboards ✅
Dashboards, top bar with logo, profile and Ride History navigation, and Ride Now / Tomorrow tabs.

### Milestone 3: Tomorrow Ride System ✅
Ride and request models, matching by route, date, direction, and time, accept/decline, matched rider details, and direct call.

### Milestone 4: Ride Now System ✅
Live rides, real-time matching, status system, start/ongoing/completion confirmations, and cancellation and expiry.

### Milestone 5: Security and Firestore Rules ✅
Role-based rules, passenger-only and rider-only actions, single active request, secure rating/report updates, and composite indexes.

### Milestone 6: Rating, Report, and Ride History ✅
Rider ratings, issue reports, profile stats, Ride History screen, and duplicate prevention.

### Milestone 7: Standalone Server, Phone Sharing, and Ride Now Fixes ✅
Node.js/Express server on Render, server-side cancellation, phone sharing in both flows, and riders going offline after trips.

### Milestone 8: Tomorrow Lifecycle, Google Sign-In, and Push Notifications ✅
Full Tomorrow trip lifecycle, Tomorrow ratings and reports, Google Sign-In with profile completion, FCM notifications, and per-transition security rules.

### Milestone 9: XP Ledger, Vehicles, and Trip Reconciliation ✅
- XP rebuilt as an evidence-backed, append-only ledger. Farming loops removed.
- Bike/car vehicle selection with per-trip seat count and vehicle details shown to passengers.
- Missed-ride reconciliation for Tomorrow trips, and force-close for stuck Ride Now trips.
- Two-way ratings, light/dark theme, guest browsing, and tablet layout fixes.
- Security fixes (`live_rides` hijack, unverified counter updates, race conditions) and Tomorrow rides now recorded in Ride History.

### Milestone 10: Website and Web Dashboard ✅
- Next.js marketing site and web dashboard on Netlify, sharing the Firebase backend.
- Redesigned homepage and a public suggestion box.

### Milestone 11: Live Map and In-Ride Chat ✅
- Offline Dhaka map (MapLibre + bundled PMTiles) with a Map tab and bottom navigation.
- Live rider/passenger location sharing through Realtime Database, a full-screen trip map with colored pins, and accurate GPS fixes from the fused location provider.
- Short-lived in-ride chat with quick replies, push notifications, and automatic deletion after 24 hours.

### Milestone 12: Final Touches and Deployment 🚧
- UI polish and full passenger/rider testing on emulator and physical devices.
- Firestore rules testing.
- Release APK and Play Store preparation.
- Expansion plan for more universities.

---

## 💻 Technologies Used

| Category | Technology |
|---|---|
| Mobile | Android (minSdk 26, targetSdk 35), Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM (ViewModel / StateFlow), Repository pattern |
| Authentication | Firebase Authentication, Google Sign-In (Credential Manager API) |
| Database | Cloud Firestore |
| Live Location | Firebase Realtime Database, Google Play Services Location (fused provider) |
| Maps | MapLibre Android SDK, offline PMTiles (Dhaka) |
| Push Notifications | Firebase Cloud Messaging |
| Background Work | WorkManager (ride reminders) |
| Backend Server | Node.js, Express, TypeScript, Firebase Admin SDK |
| Server Hosting | Render |
| Website | Next.js, React, Tailwind CSS, Firebase JS SDK |
| Web Hosting | Netlify |
| Version Control | Git, GitHub |

---

## 🚧 Installation

### Prerequisites

- Android Studio (recent stable) with Android SDK
- JDK 11+ (Android Studio's bundled JBR works)
- A Firebase project with Authentication, Firestore, Realtime Database, and Cloud Messaging enabled
- Git
- Node.js 18+ (only for running `server/` or `web/` locally)

### Android app

1. Clone the repository:
   ```bash
   git clone https://github.com/Rakibul-rahi/CholoGO.git
   ```
2. Open the project in Android Studio.
3. Put your Firebase `google-services.json` in `app/`.
4. Deploy the security rules and indexes:
   ```bash
   firebase deploy --only firestore
   ```
5. Sync Gradle and run on an emulator or device. The offline Dhaka map is bundled, so the map needs no extra setup.

---

## 👷 Team Member

| ID | Name | Email | GitHub | Role |
|---|---|---|---|---|
| 20210204077 | Rakibul Islam Rahi | rakibulislam.rahi.rir@gmail.com | Rakibul-rahi | Frontend + Backend |

---

## ✔️ Live Project

**Live App (Appetize):** https://appetize.io/app/b_jg6uxuzyfrukxtqt7p5tt2vhui

**Backend API:** https://chologo.onrender.com

**Website:** https://chologoaust.netlify.app/

**APK Link:** (https://appetize.io/app/b_6lepunxztz4iwffsb345k2lliq)

---

## 📌 Future Improvements

- University email verification.
- Student ID card verification.
- Turn-by-turn route display on the trip map.
- Rider wallet or earnings summary.
- Admin moderation panel (reports, suggestions).
- Emergency contact / SOS feature.
- Support for multiple universities.
- iOS version.

---

## 🙌 Thank You

Thank you for supporting **CholoGO**.

A student-focused ride-sharing solution built for safer, cheaper, and smarter campus transportation.
