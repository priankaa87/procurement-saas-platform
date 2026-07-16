import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import { Layout } from "./components/Layout";
import { TendersPage } from "./pages/TendersPage";
import { TenderDetailPage } from "./pages/TenderDetailPage";
import { SuppliersPage } from "./pages/SuppliersPage";
import { ReportsPage } from "./pages/ReportsPage";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        // No point retrying a 403 or 404 — the answer will not change. Retry only the
        // transient failures (network, 5xx), and not forever.
        const status = (error as { status?: number }).status;
        if (status === 403 || status === 404) return false;
        return failureCount < 2;
      },
    },
  },
});

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/tenders" replace /> },
      { path: "tenders", element: <TendersPage /> },
      { path: "tenders/:id", element: <TenderDetailPage /> },
      { path: "suppliers", element: <SuppliersPage /> },
      { path: "reports", element: <ReportsPage /> },
    ],
  },
]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
