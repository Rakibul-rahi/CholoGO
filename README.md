# CholoGO

## Table of Contents

- [Project Description](#-project-description)
- [Project Features](#-project-features)
- [Objectives](#-objectives)
- [Target Audience](#-target-audience)
- [Firebase Collections & Services](#-firebase-collections--services)
- [Backend Server (Render)](#-backend-server-render)
- [Milestones](#-milestones)
- [Technologies Used](#-technologies-used)
- [Installation](#-installation)
- [Team Members](#-team-members)
- [Live Project & Mock UI](#-live-project--mock-ui)

---

## 📝 Project Description

**CholoGO** is a student-focused ride-sharing mobile application designed for university communities. The main goal of the project is to create a safe, affordable, and closed-community ride-sharing platform where verified students can offer or request rides.

The project initially focuses on **Ahsanullah University of Science and Technology (AUST)** students. Riders are student bikers, and passengers are students looking for rides to campus or from campus to home. CholoGO provides both **Ride Now** and **Tomorrow Ride** features to support instant and scheduled ride matching.

The application is built using **Kotlin**, **Jetpack Compose**, and **Firebase**. Firebase Authentication (email/password and Google Sign-In) handles login and signup, Cloud Firestore stores users, rides, ride requests, live rides, ratings, and reports, and a small standalone **Node.js/Express server hosted on Render** handles the few operations that need administrative privileges beyond what client-side Firestore rules can safely allow — cancelling a matched ride across two users' documents, and sending push notifications through Firebase Cloud Messaging.

---

## 💡 Project Features

### i. User Authentication and Role Selection

- User registration and login using Firebase Authentication.
- **Sign in with Google** via the Credential Manager API, alongside email/password.
- Forgot password option.
- First-time Google sign-in collects role and phone number through a dedicated profile-completion step, since Google doesn't provide either.
- Role selection after authentication:
  - Passenger
  - Rider
- Separate dashboard for passengers and riders.
- User profile page.
- Role-based access and Firestore rules.

### ii. Passenger Features

- Passenger dashboard with Ride Now and Tomorrow Ride options.
- Request instant Ride Now rides.
- Search for matching live riders.
- Request scheduled rides for tomorrow.
- View accepted ride information, including the matched rider's phone number.
- Call matched rider directly.
- Confirm ride start and ride completion — for both Ride Now and Tomorrow Ride.
- Rate rider or report an issue after a completed ride — for both Ride Now and Tomorrow Ride.
- **Push notification** when a rider accepts a Tomorrow ride request.
- Notified if a rider cancels an already-accepted Tomorrow ride, with a one-tap way to resubmit the request.

### iii. Rider Features

- Rider dashboard with Ride Now and Tomorrow Ride options.
- Go live for instant ride requests.
- Select pickup and destination.
- Set ride time and available seats.
- View incoming passenger requests, including the passenger's phone number once matched.
- Accept or decline ride requests.
- Start and complete the trip with passenger confirmation at each step — for both Ride Now and Tomorrow Ride.
- Automatically goes offline after completing or cancelling a Ride Now trip — must tap "Go Live" again to receive new requests.
- Earn XP after accepting scheduled rides.
- View rider level and progress.
- **Push notification** the moment a passenger's request matches an already-saved Tomorrow ride.

### iv. Ride Now System

- Real-time ride matching between passengers and riders.
- Passenger can send only one active Ride Now request at a time.
- Rider can go live and receive matching requests.
- Matching is based on:
  - Pickup location
  - Destination
  - Route key
  - Time difference within allowed range
  - Available seats
  - Active rider status
- Ride Now status flow:
  - SEARCHING
  - NOTIFIED
  - ACCEPTED
  - START_PENDING_CONFIRMATION
  - ONGOING
  - END_PENDING_CONFIRMATION
  - COMPLETED
  - CANCELLED
  - EXPIRED
  - ISSUE_REPORTED

### v. Tomorrow Ride System

- Riders can create scheduled rides for the next day.
- Passengers can request tomorrow rides.
- Matching is based on:
  - Ride date
  - Pickup
  - Destination
  - Direction
  - Time range
  - Seat availability
- Rider can accept or decline requests.
- Passenger can see matched rider details, including phone number.
- Direct call option is available after matching, in both directions.
- Full trip lifecycle, mirroring Ride Now:
  - PENDING
  - ACCEPTED
  - START_PENDING_CONFIRMATION
  - ONGOING
  - END_PENDING_CONFIRMATION
  - COMPLETED
  - CANCELLED
- A cancelled request can be resubmitted by the passenger, returning it to PENDING so other riders can match it again.
- Rider earns XP after accepting a tomorrow ride request.

### vi. Closed Community System

- Designed for university students.
- Initial target community is AUST.
- Future expansion can include:
  - NSU
  - BRAC University
  - SEU
  - UIU
  - ULAB
- Helps create a safer ride-sharing environment by limiting users to verified student communities.

### vii. Rating and Report System

- Passengers can rate riders after a completed ride — for both Ride Now and Tomorrow Ride.
- Rating affects rider profile statistics (average rating, rating count).
- Users can report issues; report count is stored on the rider's profile for moderation.
- Buttons are disabled after rating or reporting to prevent duplicate submissions.

### viii. Ride History

- Passengers and riders can view previous rides.
- Ride history includes completed ride details.
- Helps users track previous ride activity.

### ix. Gamification System

- Riders earn XP for accepting scheduled ride requests.
- Rider level system includes:
  - Current XP
  - Current level
  - Next level progress
  - Rider title
- Encourages active participation from student riders.

### x. Push Notifications & Real-Time Alerts

- **Firebase Cloud Messaging (FCM)** delivers server-pushed notifications that reach a device even when the app is backgrounded or fully closed.
- Passenger is notified the moment a rider accepts their Tomorrow ride request.
- Rider is notified the moment a passenger's request matches an already-saved Tomorrow ride.
- On-device reminder one hour before a confirmed Tomorrow ride, asking whether it's still happening.
- Local notification if a rider cancels an already-accepted Tomorrow ride.

### xi. Responsive Mobile UI

- Built with Jetpack Compose.
- Separate screens for Passenger and Rider flows.
- Clean dashboard structure.
- Reusable shared UI components.
- Modern mobile-friendly card layouts.

---

## 🎯 Objectives

- **Create a Student-Only Ride Platform:** Build a ride-sharing system limited to university students.
- **Reduce Transportation Cost:** Provide a cheaper alternative compared to commercial ride-sharing services.
- **Improve Campus Transportation:** Help students travel to and from campus more conveniently.
- **Ensure Community Trust:** Use a closed-community model to improve safety and reliability.
- **Support Real-Time and Scheduled Rides:** Provide both instant Ride Now and Tomorrow Ride options.
- **Encourage Rider Participation:** Use XP and level systems to motivate student riders.
- **Keep Users Informed in Real Time:** Use push notifications so passengers and riders never have to keep the app open to know what's happening with their ride.
- **Build a Scalable MVP:** Create a system that can later expand to other universities.

---

## 👥 Target Audience

- AUST students who need affordable rides.
- Student bikers who want to share rides.
- University students who travel regularly between home and campus.
- Students looking for safer community-based ride-sharing.
- Future university communities in Dhaka.

---

## 📜 Firebase Collections & Services

> CholoGO uses Firebase for authentication, data, and rules, plus one small standalone server (see [Backend Server (Render)](#-backend-server-render)) for the handful of operations Firestore rules alone can't safely cover.

---

### Authentication

Firebase Authentication is used for:

- User signup (email/password).
- Sign in with Google (Credential Manager API + Firebase `GoogleAuthProvider`).
- User login.
- Forgot password.
- Authenticated user session.
- Role-based dashboard navigation.

---

### Users Collection

Firestore collection: `users`

#### User Fields

```kotlin
{
    uid: String,
    name: String,
    email: String,
    phone: String,
    role: String, // passenger or rider
    university: String,
    studentId: String,
    homeLocation: String,
    xp: Long,
    ratingAverage: Double,
    ratingCount: Int,
    reportCount: Int,
    fcmTokens: List<String>, // device tokens for push notifications
    createdAt: Long
}
```

---

### Tomorrow Rides Collection

Firestore collection: `rides`

#### Ride Fields

```kotlin
{
    riderId: String,
    riderName: String,
    tripDirection: String, // to_campus or to_home
    pickup: String,
    destination: String,
    tripTime: String,
    timeMinutes: Int,
    routeKey: String,
    rideDate: String,
    availableSeats: Int,
    status: String, // active, full, cancelled
    isTomorrowSetup: Boolean,
    createdAt: Timestamp
}
```

---

### Tomorrow Ride Requests Collection

Firestore collection: `ride_requests`

#### Ride Request Fields

```kotlin
{
    requestId: String,
    userId: String,
    passengerName: String,
    passengerPhone: String,
    pickup: String,
    destination: String,
    tripDirection: String,
    tripTime: String,
    hour: Int,
    minute: Int,
    timeMinutes: Int,
    routeKey: String,
    rideDate: String,
    status: String, // pending, accepted, start_pending_confirmation, ongoing, end_pending_confirmation, completed, cancelled
    matchedRideId: String,
    matchedRiderId: String,
    matchedRiderName: String,
    matchedRiderPhone: String,
    matchedRideTime: String,
    acceptedAt: Timestamp,
    rideStartedByRider: Boolean,
    rideConfirmedByPassenger: Boolean,
    rideEndedByRider: Boolean,
    rideCompletedByPassenger: Boolean,
    startedAt: Timestamp,
    completedAt: Timestamp,
    riderRated: Boolean,
    rating: Int,
    ratedAt: Timestamp,
    issueReported: Boolean,
    reportReason: String,
    reportDetails: String,
    reportedAt: Timestamp,
    rejectedByRiderIds: List<String>,
    cancelledBy: String,
    cancelledByRole: String, // rider or passenger
    cancellationReason: String,
    cancelledAt: Timestamp,
    createdAt: Timestamp
}
```

---

### Live Rides Collection

Firestore collection: `live_rides`

#### Live Ride Fields

```kotlin
{
    rideId: String,
    riderId: String,
    riderName: String,
    pickup: String,
    destination: String,
    tripDirection: String,
    tripTime: String,
    timeMinutes: Int,
    routeKey: String,
    availableSeats: Int,
    status: String, // active, inactive
    isLiveNow: Boolean,
    isAvailable: Boolean,
    currentRequestId: String,
    createdAt: Timestamp,
    lastUpdatedAt: Timestamp
}
```

---

### Ride Now Requests Collection

Firestore collection: `ride_now_requests`

#### Ride Now Request Fields

```kotlin
{
    requestId: String,
    passengerId: String,
    passengerName: String,
    passengerPhone: String,
    pickup: String,
    destination: String,
    tripTime: String,
    timeMinutes: Int,
    routeKey: String,
    status: String, // searching, notified, accepted, start_pending_confirmation, ongoing, end_pending_confirmation, completed, cancelled, expired, issue_reported
    matchedRideId: String,
    matchedRiderId: String,
    matchedRiderName: String,
    matchedRiderPhone: String,
    acceptedAt: Timestamp,
    startedAt: Timestamp,
    completedAt: Timestamp,
    cancelledAt: Timestamp,
    expiredAt: Timestamp,
    createdAt: Timestamp,
    expiresAt: Timestamp,
    riderRated: Boolean,
    rating: Int,
    ratedAt: Timestamp,
    issueReported: Boolean,
    reportReason: String,
    reportDetails: String,
    reportedAt: Timestamp
}
```

---

### Ride Ratings Collection

Firestore collection: `ride_ratings`

#### Rating Fields

```kotlin
{
    ratingId: String,
    requestId: String,
    rideId: String,
    passengerId: String,
    riderId: String,
    ratedBy: String,
    ratedTo: String,
    stars: Int, // 1-5
    comment: String,
    createdAt: Timestamp
}
```

---

### Ride Reports Collection

Firestore collection: `ride_reports`

#### Report Fields

```kotlin
{
    reportId: String,
    requestId: String,
    rideId: String,
    passengerId: String,
    riderId: String,
    reportedBy: String,
    reportedUserId: String,
    reason: String,
    details: String,
    status: String, // pending
    createdAt: Timestamp
}
```

---

### Firestore Security Rules

All rules live in [`firestore.rules`](firestore.rules) and are version-controlled alongside the app rather than only living in the Firebase console. Instead of one broad rule per collection, every state transition (accept, start, confirm, complete, cancel, rate, report) has its own tightly scoped function that pins the exact prior status, the new status, and the exact set of fields allowed to change — so, for example, a rider can only accept a still-pending request by matching themselves, and can't smuggle unrelated field edits into that same write.

---

## 🌐 Backend Server (Render)

CholoGO is primarily serverless — almost everything goes straight from the app to Firestore under the rules above. A small standalone **Node.js + Express** server, using the **Firebase Admin SDK**, covers the few things a client can't safely be trusted to do on its own:

- Cancelling a passenger's already-accepted request, which needs to update *two* documents the passenger doesn't own outright (their own request, and the matched rider's ride/seat count) in one transaction.
- Sending **Firebase Cloud Messaging** push notifications, which requires Admin SDK credentials no client should ever hold.

**Hosting:** deployed on [Render](https://render.com)'s free tier, auto-deploying from the `main` branch of this repository.

**Live URL:** https://chologo.onrender.com

> ⚠️ The free tier spins the instance down after 15 minutes of inactivity, so the first request after a period of idleness can take 30–60 seconds to wake it back up. The Android app's HTTP client is configured with generous timeouts to accommodate this.

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/tomorrow/cancel-request` | Cancels the caller's own accepted Tomorrow request and restores the matched rider's seat, in one transaction. |
| `POST` | `/api/tomorrow/notify-accepted` | Called by the accepting rider right after their accept succeeds; pushes the passenger a "request accepted" notification. |
| `POST` | `/api/tomorrow/notify-match` | Called by a passenger right after submitting a request; finds any rider whose saved ride matches it and pushes them a notification. |
| `GET` | `/health` | Basic health check. |

All `/api/tomorrow/*` endpoints require a Firebase ID token in the `Authorization: Bearer <token>` header, and independently re-verify on the server that the caller actually owns the relevant side of the request before doing anything — the client can't spoof another user's uid.

### Local development

```bash
cd server
npm install
npm run dev      # ts-node-dev, live reload
npm run build    # type-check + compile to dist/
npm start        # run the compiled server
```

The server needs a `FIREBASE_SERVICE_ACCOUNT` environment variable — the JSON contents of a Firebase service account key for the project, kept out of source control and set directly in Render's environment settings (and your own shell/`.env` for local runs).

---

## 📝 Milestones

### Milestone 1: Initial Setup and Authentication

- Set up Android Studio project.
- Configure Kotlin and Jetpack Compose.
- Connect Firebase project.
- Add Firebase Authentication.
- Create signup screen.
- Create login screen.
- Add forgot password screen.
- Add role selection screen.
- Navigate users based on Passenger or Rider role.

### Milestone 2: Basic Passenger and Rider Dashboards

- Create Passenger Dashboard.
- Create Rider Dashboard.
- Create top bar with CholoGO logo.
- Add profile navigation.
- Add Ride History navigation.
- Add separate tabs for Ride Now and Tomorrow Ride.

### Milestone 3: Tomorrow Ride System

- Create ride model.
- Create ride request model.
- Allow riders to create tomorrow rides.
- Allow passengers to request tomorrow rides.
- Match rides using route, date, direction, and time.
- Allow riders to accept or decline requests.
- Show matched rider details to passenger.
- Add direct call option.
- Add XP reward for riders.

### Milestone 4: Ride Now System

- Create LiveRide model.
- Create RideNowRequest model.
- Allow riders to go live.
- Allow passengers to search for live rides.
- Match live rides based on route and time.
- Add request status system.
- Add accepted ride card.
- Add ride start confirmation.
- Add ongoing ride state.
- Add ride completion confirmation.
- Add cancellation and expiry handling.

### Milestone 5: Security and Firestore Rules

- Add role-based Firestore rules.
- Restrict passenger-only actions.
- Restrict rider-only actions.
- Prevent multiple active passenger requests.
- Prevent rider from stopping live ride after accepting a request.
- Add secure rating and report update rules.
- Create required Firestore composite indexes.

### Milestone 6: Rating, Report, and Ride History

- Add rider rating system.
- Add issue report system.
- Update user rating average and rating count.
- Update user report count.
- Create Ride History screen.
- Display completed ride details.
- Disable duplicate rating and reporting.

### Milestone 7: Standalone Server, Phone Sharing, and Ride Now Fixes

- Add the standalone Node.js/Express REST server, deployed on Render.
- Move passenger-initiated cancellation to the server, restoring the rider's seat synchronously.
- Share phone numbers between matched passenger and rider, for both Ride Now and Tomorrow Ride.
- Fix riders not going offline after completing or cancelling a Ride Now trip.
- Fix a cancelled Tomorrow request being stuck and unable to be resubmitted.

### Milestone 8: Tomorrow Ride Lifecycle, Google Sign-In, and Push Notifications

- Bring the full start/ongoing/completion confirmation flow to Tomorrow Ride, mirroring Ride Now.
- Add rating and reporting for Tomorrow Ride, reusing the existing rating/report system.
- Add Google Sign-In, including first-time profile completion (role and phone) for new Google accounts.
- Add Firebase Cloud Messaging push notifications: rider-accepted (to the passenger) and new-match (to the rider), sent via the standalone server.
- Rewrite Firestore security rules with per-transition scoped functions instead of broad collection-level rules, and bring `firestore.rules`/`firestore.indexes.json` into version control.
- Various bug fixes and signup validation improvements.

### Milestone 9: Final Touches and Future Deployment

- Improve UI design.
- Test passenger and rider flows.
- Test Firestore rules.
- Test on emulator and physical device.
- Fix bugs.
- Prepare APK for testing.
- Prepare for Play Store deployment.
- Plan future expansion to more universities.

---

## 💻 Technologies Used

| Category | Technology |
|---|---|
| Mobile Development | Android |
| Programming Language | Kotlin |
| UI Framework | Jetpack Compose |
| Backend as a Service | Firebase |
| Authentication | Firebase Authentication, Google Sign-In (Credential Manager API) |
| Database | Cloud Firestore |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Backend Server | Node.js, Express, TypeScript |
| Server SDK | Firebase Admin SDK |
| Server Hosting | Render |
| State Management | ViewModel / StateFlow |
| Architecture | Repository Pattern |
| Navigation | Navigation Compose |
| UI Components | Material 3 |
| Version Control | Git |
| Repository | GitHub |
| Platform | Android |
| Rendering Method | Native Mobile UI |

---

## 🚧 Installation

### Prerequisites

Before running this project, make sure you have installed:

- Android Studio
- JDK 17 or compatible version
- Android SDK
- Kotlin
- Firebase project
- Git
- Node.js 18+ (only needed if you're running the [backend server](#-backend-server-render) locally)

## 👷 Team Member

| ID | Name | Email | GitHub | Role |
|---|---|---|---|---|
| 20210204077 | Rakibul Islam Rahi | rakibulislam.rahi.rir@gmail.com | Rakibul-rahi | Frontend + Backend |

---

## ✔️ Live Project 



**Live Project Link:** https://appetize.io/app/b_jg6uxuzyfrukxtqt7p5tt2vhui

**Backend API:** https://chologo.onrender.com

**APK Link:** Not added yet

---

## 📌 Future Improvements

- Add university email verification.
- Add student ID card verification.
- Add map integration.
- Add live location tracking.
- Add ad banners and sponsor cards.
- Add rider wallet or earnings summary.
- Add admin moderation panel.
- Add emergency contact feature.
- Add support for multiple universities.
- Add iOS version in the future.

---

## 🙌 Thank You

Thank you for supporting **CholoGO**.

A student-focused ride-sharing solution built for safer, cheaper, and smarter campus transportation.
