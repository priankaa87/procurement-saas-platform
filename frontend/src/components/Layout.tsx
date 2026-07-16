import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/useAuth";

const NAV = [
  { to: "/tenders", label: "Tenders" },
  { to: "/suppliers", label: "Suppliers" },
  { to: "/reports", label: "Reports" },
];

export function Layout() {
  const { user, signedIn } = useAuth();
  return (
    <div style={{ fontFamily: "system-ui, sans-serif", color: "#0f172a" }}>
      <header
        style={{
          display: "flex",
          alignItems: "center",
          gap: 24,
          padding: "12px 24px",
          borderBottom: "1px solid #e2e8f0",
        }}
      >
        <strong style={{ fontSize: 18 }}>Procurement SaaS</strong>
        <nav style={{ display: "flex", gap: 16, flex: 1 }}>
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              style={({ isActive }) => ({
                textDecoration: "none",
                color: isActive ? "#1e40af" : "#475569",
                fontWeight: isActive ? 700 : 500,
              })}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <span style={{ fontSize: 13, color: "#64748b" }}>
          {signedIn ? `${user?.username} · ${user?.tenant}` : "Signed out"}
        </span>
      </header>
      <main style={{ padding: 24, maxWidth: 1100, margin: "0 auto" }}>
        {signedIn ? (
          <Outlet />
        ) : (
          <p style={{ color: "#64748b" }}>
            Signed out. Set <code>VITE_DEV_TOKEN</code> to a Keycloak token to explore, or
            wire the OIDC login in <code>src/auth</code>.
          </p>
        )}
      </main>
    </div>
  );
}
