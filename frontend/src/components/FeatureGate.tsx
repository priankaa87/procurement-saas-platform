import type { ReactNode } from "react";
import { useAuth } from "../auth/useAuth";

interface Props {
  feature: string;
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * Shows its children only if the user holds the feature.
 *
 * This hides UI a user cannot use — a courtesy, not a control. The action behind the UI is
 * still guarded server-side by the same feature, so hiding it wrong is a cosmetic bug, not
 * a security hole.
 */
export function FeatureGate({ feature, children, fallback = null }: Props) {
  const { can } = useAuth();
  return <>{can(feature) ? children : fallback}</>;
}
