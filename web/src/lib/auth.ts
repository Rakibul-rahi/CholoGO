import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
} from "firebase/auth";
import { doc, setDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";

export interface PendingSignup {
  name: string;
  email: string;
  phone: string;
  studentId: string;
  university: string;
}

const PENDING_SIGNUP_KEY = "chologo_pending_signup";

export async function signUp(params: {
  name: string;
  email: string;
  phone: string;
  studentId: string;
  university: string;
  password: string;
}) {
  const credential = await createUserWithEmailAndPassword(
    auth,
    params.email,
    params.password
  );
  await updateProfile(credential.user, { displayName: params.name });

  const pending: PendingSignup = {
    name: params.name,
    email: params.email,
    phone: params.phone,
    studentId: params.studentId,
    university: params.university,
  };
  if (typeof window !== "undefined") {
    sessionStorage.setItem(PENDING_SIGNUP_KEY, JSON.stringify(pending));
  }

  return credential.user;
}

export function readPendingSignup(): PendingSignup | null {
  if (typeof window === "undefined") return null;
  const raw = sessionStorage.getItem(PENDING_SIGNUP_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as PendingSignup;
  } catch {
    return null;
  }
}

export function clearPendingSignup() {
  if (typeof window !== "undefined") {
    sessionStorage.removeItem(PENDING_SIGNUP_KEY);
  }
}

export async function completeProfile(params: {
  uid: string;
  name: string;
  email: string;
  phone: string;
  studentId: string;
  university: string;
  role: "passenger" | "rider";
  vehicleType?: string;
  vehicleModel?: string;
  vehicleNumber?: string;
  vehicleColor?: string;
}) {
  await setDoc(doc(db, "users", params.uid), {
    uid: params.uid,
    name: params.name,
    email: params.email,
    phone: params.phone,
    role: params.role,
    vehicleType: params.vehicleType ?? "",
    vehicleModel: params.vehicleModel ?? "",
    vehicleNumber: params.vehicleNumber ?? "",
    vehicleColor: params.vehicleColor ?? "",
    university: params.university || "AUST",
    studentId: params.studentId,
    homeLocation: "",
    xp: 0,
    createdAt: Date.now(),
    ratingAverage: 0,
    ratingCount: 0,
    reportCount: 0,
    completedRideCount: 0,
    fcmTokens: [],
  });

  clearPendingSignup();
}

export function signIn(email: string, password: string) {
  return signInWithEmailAndPassword(auth, email, password);
}

export function signOutUser() {
  return signOut(auth);
}
