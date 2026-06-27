# CholoGO

## Table of Contents

- [Project Description](#-project-description)
- [Project Features](#-project-features)
- [Objectives](#-objectives)
- [Target Audience](#-target-audience)
- [Firebase Collections & Services](#-firebase-collections--services)
- [Milestones](#-milestones)
- [Technologies Used](#-technologies-used)
- [Installation](#-installation)
- [Team Members](#-team-members)
- [Live Project & Mock UI](#-live-project--mock-ui)

---

## 📝 Project Description

**CholoGO** is a student-focused ride-sharing mobile application designed for university communities. The main goal of the project is to create a safe, affordable, and closed-community ride-sharing platform where verified students can offer or request rides.

The project initially focuses on **Ahsanullah University of Science and Technology (AUST)** students. Riders are student bikers, and passengers are students looking for rides to campus or from campus to home. CholoGO provides both **Ride Now** and **Tomorrow Ride** features to support instant and scheduled ride matching.

The application is built using **Kotlin**, **Jetpack Compose**, and **Firebase**. Firebase Authentication is used for user login and signup, while Cloud Firestore is used to store users, rides, ride requests, live rides, ratings, and reports.

---

## 💡 Project Features

### i. User Authentication and Role Selection

- User registration and login using Firebase Authentication.
- Forgot password option.
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
- View accepted ride information.
- Call matched rider directly.
- Confirm ride start.
- Confirm ride completion.
- Rate rider after completed ride.
- Report issue if a problem occurs.

### iii. Rider Features

- Rider dashboard with Ride Now and Tomorrow Ride options.
- Go live for instant ride requests.
- Select pickup and destination.
- Set ride time and available seats.
- View incoming passenger requests.
- Accept or decline ride requests.
- Confirm ride start.
- Complete ride after passenger confirmation.
- Earn XP after accepting scheduled rides.
- View rider level and progress.

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
- Passenger can see matched rider details.
- Direct call option is available after matching.
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

- Passengers can rate riders after a completed ride.
- Rating affects rider profile statistics.
- Users can report issues.
- Report count is stored for moderation.
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

### x. Responsive Mobile UI

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

> CholoGO uses Firebase services instead of a traditional REST API backend.

---

### Authentication

Firebase Authentication is used for:

- User signup.
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
    role: String, // Passenger or Rider
    ratingAverage: Double,
    ratingCount: Int,
    reportCount: Int,
    createdAt: Timestamp
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
    pickup: String,
    destination: String,
    tripDirection: String,
    tripTime: String,
    hour: Int,
    minute: Int,
    timeMinutes: Int,
    routeKey: String,
    rideDate: String,
    status: String, // pending, accepted, cancelled
    matchedRideId: String,
    matchedRiderId: String,
    matchedRiderName: String,
    matchedRiderPhone: String,
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
    status: String, // active, full, cancelled
    isLiveNow: Boolean,
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
    pickup: String,
    destination: String,
    tripTime: String,
    timeMinutes: Int,
    routeKey: String,
    status: String,
    matchedRideId: String,
    matchedRiderId: String,
    matchedRiderName: String,
    matchedRiderPhone: String,
    acceptedAt: Timestamp,
    cancelledAt: Timestamp,
    createdAt: Timestamp,
    expiresAt: Timestamp,
    startedAt: Timestamp,
    completedAt: Timestamp,
    expiredAt: Timestamp,
    riderRated: Boolean,
    issueReported: Boolean
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
    passengerId: String,
    riderId: String,
    rating: Int,
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
    reporterId: String,
    reportedUserId: String,
    reportReason: String,
    reportDetails: String,
    createdAt: Timestamp
}
```

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

### Milestone 7: Final Touches and Future Deployment

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
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
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

## 👷 Team Member

| ID | Name | Email | GitHub | Role |
|---|---|---|---|---|
| 20210204077 | Rakibul Islam Rahi | rakibulislam.rahi.rir@gmail.com | Rakibul-rahi | Frontend + Backend |

---

## ✔️ Live Project 



**Live Project Link:** Not deployed yet

**APK Link:** Not added yet

---

## 📌 Future Improvements

- Add university email verification.
- Add student ID card verification.
- Add map integration.
- Add live location tracking.
- Add push notifications.
- Add reminder system for Tomorrow Ride.
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
