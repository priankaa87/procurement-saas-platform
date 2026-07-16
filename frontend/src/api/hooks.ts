import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client";
import type {
  Participant,
  ReportDefinition,
  ReportJob,
  Supplier,
  Tender,
  TenderItem,
} from "./types";

// One hook per endpoint. Pages call these rather than fetch directly, so caching, retries,
// and loading/error handling are uniform and live in exactly one place.

export function useTenders(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return useQuery({
    queryKey: ["tenders", status ?? "all"],
    queryFn: () => api<Tender[]>(`/api/tenders${query}`),
  });
}

export function useTender(id: number) {
  return useQuery({
    queryKey: ["tender", id],
    queryFn: () => api<Tender>(`/api/tenders/${id}`),
  });
}

export function useTenderItems(id: number) {
  return useQuery({
    queryKey: ["tender", id, "items"],
    queryFn: () => api<TenderItem[]>(`/api/tenders/${id}/items`),
  });
}

export function useTenderParticipants(id: number) {
  return useQuery({
    queryKey: ["tender", id, "participants"],
    queryFn: () => api<Participant[]>(`/api/tenders/${id}/participants`),
  });
}

export function useSuppliers(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return useQuery({
    queryKey: ["suppliers", status ?? "all"],
    queryFn: () => api<Supplier[]>(`/api/vendors/suppliers${query}`),
  });
}

export function useReportDefinitions() {
  return useQuery({
    queryKey: ["report-definitions"],
    queryFn: () => api<ReportDefinition[]>(`/api/reports/definitions`),
  });
}

export function useReportJobs() {
  return useQuery({
    queryKey: ["report-jobs"],
    queryFn: () => api<ReportJob[]>(`/api/reports/jobs`),
    // A running job finishes on the server; poll so the UI reflects it without a reload.
    refetchInterval: (query) => {
      const jobs = query.state.data as ReportJob[] | undefined;
      const anyRunning = jobs?.some((j) => j.status === "QUEUED" || j.status === "RUNNING");
      return anyRunning ? 2000 : false;
    },
  });
}

export function useRunReport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (definitionCode: string) =>
      api<ReportJob>(`/api/reports`, {
        method: "POST",
        body: JSON.stringify({ definitionCode, parameters: {} }),
      }),
    // The new job should appear immediately, so refresh the list once it is accepted.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["report-jobs"] }),
  });
}
