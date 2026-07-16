import { useNavigate } from "react-router-dom";
import { useTenders } from "../api/hooks";
import { Async } from "../components/Async";
import { DataTable } from "../components/DataTable";
import { StatusPill } from "../components/StatusPill";
import type { Tender } from "../api/types";

export function TendersPage() {
  const navigate = useNavigate();
  const { data, isLoading, error } = useTenders();

  return (
    <section>
      <h1 style={{ fontSize: 22 }}>Tenders</h1>
      <Async loading={isLoading} error={error} data={data}>
        {(tenders) => (
          <DataTable<Tender>
            rows={tenders}
            keyOf={(t) => t.id}
            onRowClick={(t) => navigate(`/tenders/${t.id}`)}
            empty="No tenders yet."
            columns={[
              { header: "Code", render: (t) => <strong>{t.code}</strong> },
              { header: "Title", render: (t) => t.title },
              { header: "Status", render: (t) => <StatusPill status={t.status} /> },
              { header: "Deadline", render: (t) => new Date(t.bidDeadline).toLocaleString() },
              { header: "Items", align: "right", render: (t) => t.itemCount },
            ]}
          />
        )}
      </Async>
    </section>
  );
}
