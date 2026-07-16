// Maps the various status vocabularies to a small set of tones, so a glance across any
// table reads the same way: green is settled/good, amber is in-flight, red is stopped.

type Tone = "neutral" | "progress" | "good" | "warn" | "bad";

const TONES: Record<string, Tone> = {
  // tender
  DRAFT: "neutral",
  PUBLISHED: "progress",
  UNDER_EVALUATION: "progress",
  AWARDED: "good",
  CANCELLED: "bad",
  // supplier / enlistment
  ACTIVE: "good",
  SUSPENDED: "warn",
  DEBARRED: "bad",
  EXPIRED: "warn",
  REVOKED: "bad",
  // report jobs
  QUEUED: "neutral",
  RUNNING: "progress",
  COMPLETED: "good",
  FAILED: "bad",
};

const COLOURS: Record<Tone, { bg: string; fg: string }> = {
  neutral: { bg: "#e2e8f0", fg: "#334155" },
  progress: { bg: "#dbeafe", fg: "#1e40af" },
  good: { bg: "#dcfce7", fg: "#166534" },
  warn: { bg: "#fef9c3", fg: "#854d0e" },
  bad: { bg: "#fee2e2", fg: "#991b1b" },
};

export function StatusPill({ status }: { status: string }) {
  const tone = TONES[status] ?? "neutral";
  const { bg, fg } = COLOURS[tone];
  return (
    <span
      style={{
        backgroundColor: bg,
        color: fg,
        padding: "2px 10px",
        borderRadius: 999,
        fontSize: 12,
        fontWeight: 600,
        whiteSpace: "nowrap",
      }}
    >
      {status.replace(/_/g, " ")}
    </span>
  );
}
