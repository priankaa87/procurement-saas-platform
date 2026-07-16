# Procurement SaaS — Front End

React + TypeScript (Vite) single-page app for the platform. It talks only to the API
gateway, never to services directly, and renders UI according to the **feature codes** in
the signed-in user's token — the same feature-level model the back end enforces.

## Design decisions

- **The gateway is the only backend.** `VITE_API_BASE` points at the gateway; every request
  carries the bearer token and the API surface is exactly what the gateway exposes.
- **The UI is feature-gated, but never trusts itself.** Hiding a button a user cannot use is
  a courtesy; the back end enforces the same feature with `@PreAuthorize`. The front end
  reproducing that check is UX, not security — so a tampered token buys nothing.
- **Auth is OIDC-ready, not faked.** In development the token comes from `VITE_DEV_TOKEN`
  (or none). Wiring Keycloak's PKCE flow replaces one module (`src/auth`) and touches
  nothing else.
- **Server state lives in React Query**, not in hand-rolled `useEffect` fetches — caching,
  retries, and loading/error states come for free and consistently.

## Run

```bash
npm install
VITE_API_BASE=http://localhost:8080 npm run dev
```

## Layout

```
src/
├── api/         # typed client + endpoint hooks (React Query)
├── auth/        # token source + decoded claims (features, roles, tenant)
├── components/  # shared UI (feature gate, status pill, layout)
├── pages/       # tenders, tender detail, suppliers, reports
└── main.tsx     # router + providers
```

## Build

```bash
npm run build     # type-check + production bundle into dist/
```

`dist/` is static; deploy it to any CDN. It needs no server of its own — it calls the
gateway.
