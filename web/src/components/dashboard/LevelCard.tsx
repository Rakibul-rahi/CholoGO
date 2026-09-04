import { getLevelInfo } from "@/lib/xp";

export default function LevelCard({
  xp,
  userName,
  loading,
}: {
  xp: number;
  userName: string;
  loading: boolean;
}) {
  const info = getLevelInfo(xp);

  return (
    <div className="rounded-2xl border border-line bg-card p-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-ink-soft">
            {userName || "Rider"}
          </p>
          <p className="text-lg font-semibold">
            Level {loading ? 1 : info.level} · {loading ? "Beginner" : info.levelTitle}
          </p>
        </div>
        <p className="text-sm font-medium text-ink-soft">
          {loading ? 0 : info.currentXp} XP
        </p>
      </div>

      <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-line-strong">
        <div
          className="h-full rounded-full bg-accent transition-all"
          style={{ width: `${loading ? 0 : info.progressFraction * 100}%` }}
        />
      </div>

      <p className="mt-2 text-xs text-ink-soft">
        {loading
          ? "Loading XP..."
          : `${info.xpNeededForNextLevel} XP to next level`}
      </p>
    </div>
  );
}
