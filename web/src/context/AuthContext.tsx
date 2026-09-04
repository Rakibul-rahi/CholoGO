"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { onAuthStateChanged, type User as FirebaseUser } from "firebase/auth";
import { doc, onSnapshot } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";

export interface UserProfile {
  uid: string;
  name: string;
  email: string;
  phone: string;
  role: "passenger" | "rider" | "";
  vehicleType: string;
  vehicleModel: string;
  vehicleNumber: string;
  vehicleColor: string;
  university: string;
  studentId: string;
  homeLocation: string;
  ratingAverage: number;
  ratingCount: number;
  completedRideCount: number;
}

export type AuthStatus = "loading" | "signed-out" | "onboarding" | "ready";

interface AuthContextValue {
  status: AuthStatus;
  firebaseUser: FirebaseUser | null;
  profile: UserProfile | null;
}

const AuthContext = createContext<AuthContextValue>({
  status: "loading",
  firebaseUser: null,
  profile: null,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [firebaseUser, setFirebaseUser] = useState<FirebaseUser | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);

  useEffect(() => {
    return onAuthStateChanged(auth, (user) => {
      setFirebaseUser(user);
      setAuthLoading(false);
    });
  }, []);

  useEffect(() => {
    if (!firebaseUser) {
      setProfile(null);
      return;
    }

    setProfileLoading(true);
    return onSnapshot(doc(db, "users", firebaseUser.uid), (snap) => {
      setProfile(snap.exists() ? (snap.data() as UserProfile) : null);
      setProfileLoading(false);
    });
  }, [firebaseUser]);

  let status: AuthStatus;
  if (authLoading || (firebaseUser && profileLoading)) {
    status = "loading";
  } else if (!firebaseUser) {
    status = "signed-out";
  } else if (!profile) {
    status = "onboarding";
  } else {
    status = "ready";
  }

  return (
    <AuthContext.Provider value={{ status, firebaseUser, profile }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
