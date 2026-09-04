import { collection, onSnapshot, query, where, type Unsubscribe } from "firebase/firestore";
import { db } from "@/lib/firebase";

export interface LevelInfo {
  level: number;
  currentXp: number;
  currentLevelStartXp: number;
  nextLevelXp: number;
  progressFraction: number;
  xpNeededForNextLevel: number;
  levelTitle: string;
}

const LEVEL_THRESHOLDS = [0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200];

const LEVEL_TITLES: Record<number, string> = {
  1: "Beginner",
  2: "Explorer",
  3: "Regular",
  4: "Trusted",
  5: "Advanced",
  6: "Expert",
  7: "Elite",
  8: "Master",
  9: "Champion",
  10: "Legend",
};

export function getLevelTitle(level: number): string {
  return LEVEL_TITLES[level] ?? "Ultimate";
}

export function getLevelInfo(xp: number): LevelInfo {
  const safeXp = Math.max(xp, 0);

  let level = 1;
  for (let i = 0; i < LEVEL_THRESHOLDS.length; i++) {
    if (safeXp >= LEVEL_THRESHOLDS[i]) {
      level = i + 1;
    } else {
      break;
    }
  }

  const currentLevelStartXp = LEVEL_THRESHOLDS[level - 1];
  const nextLevelXp =
    level < LEVEL_THRESHOLDS.length
      ? LEVEL_THRESHOLDS[level]
      : currentLevelStartXp + 1000;

  const progressInLevel = safeXp - currentLevelStartXp;
  const levelRange = Math.max(nextLevelXp - currentLevelStartXp, 1);
  const progressFraction = Math.min(Math.max(progressInLevel / levelRange, 0), 1);
  const xpNeededForNextLevel = Math.max(nextLevelXp - safeXp, 0);

  return {
    level,
    currentXp: safeXp,
    currentLevelStartXp,
    nextLevelXp,
    progressFraction,
    xpNeededForNextLevel,
    levelTitle: getLevelTitle(level),
  };
}

export function listenTotalXp(
  userId: string,
  onData: (total: number) => void
): Unsubscribe {
  const xpEventsQuery = query(
    collection(db, "xp_events"),
    where("userId", "==", userId)
  );

  return onSnapshot(xpEventsQuery, (snapshot) => {
    const total = snapshot.docs.reduce(
      (sum, docSnap) => sum + (docSnap.data().amount ?? 0),
      0
    );
    onData(total);
  });
}
