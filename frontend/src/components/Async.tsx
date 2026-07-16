import type { ReactNode } from "react";
import { ApiError } from "../api/client";

interface Props<T> {
  loading: boolean;
  error: unknown;
  data: T | undefined;
  children: (data: T) => ReactNode;
}

/**
 * Renders the three states of a data fetch consistently: loading, error, and content.
 *
 * A 403 gets its own message. "You do not have access to this" and "something went wrong"
 * are different problems, and a user who sees the latter for the former files a bug that
 * was never one.
 */
export function Async<T>({ loading, error, data, children }: Props<T>) {
  if (loading) return <p style={{ color: "#64748b" }}>Loading…</p>;
  if (error) {
    const forbidden = error instanceof ApiError && error.status === 403;
    return (
      <p style={{ color: "#991b1b" }}>
        {forbidden
          ? "You do not have access to this."
          : `Could not load: ${(error as Error).message}`}
      </p>
    );
  }
  if (data === undefined) return null;
  return <>{children(data)}</>;
}
