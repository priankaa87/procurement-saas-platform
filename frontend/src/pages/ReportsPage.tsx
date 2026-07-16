import { useReportDefinitions, useReportJobs, useRunReport } from "../api/hooks";
import { Async } from "../components/Async";
import { DataTable } from "../components/DataTable";
import { FeatureGate } from "../components/FeatureGate";
import { StatusPill } from "../components/StatusPill";
import type { ReportDefinition, ReportJob } from "../api/types";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export function ReportsPage() {
  const definitions = useReportDefinitions();
  const jobs = useReportJobs();
  const run = useRunReport();

  return (
    <section>
      <h1 style={{ fontSize: 22 }}>Reports</h1>

      <h2 style={{ fontSize: 16 }}>Available reports</h2>
      <Async loading={definitions.isLoading} error={definitions.error} data={definitions.data}>
        {(defs) => (
          <DataTable<ReportDefinition>
            rows={defs}
            keyOf={(d) => d.id}
            empty="No reports configured."
            columns={[
              { header: "Report", render: (d) => d.name },
              { header: "Format", render: (d) => d.format },
              {
                header: "",
                align: "right",
                render: (d) => (
                  <FeatureGate feature="FEATURE_REPORT_RUN">
                    <button
                      onClick={() => run.mutate(d.code)}
                      disabled={run.isPending}
                      style={runButton}
                    >
                      Run
                    </button>
                  </FeatureGate>
                ),
              },
            ]}
          />
        )}
      </Async>

      <h2 style={{ fontSize: 16, marginTop: 28 }}>Recent jobs</h2>
      <p style={{ color: "#64748b", fontSize: 13, marginTop: 0 }}>
        A queued report renders in the background; this list refreshes itself until it is
        ready to download.
      </p>
      <Async loading={jobs.isLoading} error={jobs.error} data={jobs.data}>
        {(rows) => (
          <DataTable<ReportJob>
            rows={rows}
            keyOf={(j) => j.id}
            empty="No reports run yet."
            columns={[
              { header: "Report", render: (j) => j.definitionCode },
              { header: "Status", render: (j) => <StatusPill status={j.status} /> },
              {
                header: "Rows",
                align: "right",
                render: (j) => (j.rowCount != null ? j.rowCount : "—"),
              },
              {
                header: "",
                align: "right",
                render: (j) =>
                  j.downloadable ? (
                    <a href={`${API_BASE}/api/reports/jobs/${j.id}/download`} style={link}>
                      Download
                    </a>
                  ) : j.status === "FAILED" ? (
                    <span title={j.error ?? ""} style={{ color: "#991b1b", fontSize: 13 }}>
                      Failed
                    </span>
                  ) : (
                    "—"
                  ),
              },
            ]}
          />
        )}
      </Async>
    </section>
  );
}

const runButton = {
  padding: "4px 12px",
  border: "1px solid #1e40af",
  borderRadius: 6,
  background: "white",
  color: "#1e40af",
  cursor: "pointer",
  fontSize: 13,
} as const;

const link = { color: "#1e40af", fontSize: 13, fontWeight: 600 } as const;
