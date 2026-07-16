import { Link, useParams } from "react-router-dom";
import { useTender, useTenderItems, useTenderParticipants } from "../api/hooks";
import { Async } from "../components/Async";
import { DataTable } from "../components/DataTable";
import { StatusPill } from "../components/StatusPill";
import type { Participant, TenderItem } from "../api/types";

export function TenderDetailPage() {
  const { id } = useParams();
  const tenderId = Number(id);
  const tender = useTender(tenderId);
  const items = useTenderItems(tenderId);
  const participants = useTenderParticipants(tenderId);

  return (
    <section>
      <Link to="/tenders" style={{ fontSize: 13, color: "#1e40af" }}>
        ← All tenders
      </Link>
      <Async loading={tender.isLoading} error={tender.error} data={tender.data}>
        {(t) => (
          <>
            <h1 style={{ fontSize: 22, marginBottom: 4 }}>
              {t.code} <StatusPill status={t.status} />
            </h1>
            <p style={{ color: "#475569", marginTop: 0 }}>{t.title}</p>
            <dl style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "4px 16px" }}>
              <dt style={dt}>Currency</dt>
              <dd style={dd}>{t.currencyCode}</dd>
              <dt style={dt}>Bid deadline</dt>
              <dd style={dd}>{new Date(t.bidDeadline).toLocaleString()}</dd>
              {t.awardedSupplierCode && (
                <>
                  <dt style={dt}>Awarded to</dt>
                  <dd style={dd}>{t.awardedSupplierCode}</dd>
                </>
              )}
            </dl>
          </>
        )}
      </Async>

      <h2 style={h2}>Items</h2>
      <Async loading={items.isLoading} error={items.error} data={items.data}>
        {(rows) => (
          <DataTable<TenderItem>
            rows={rows}
            keyOf={(i) => i.id}
            empty="No items."
            columns={[
              { header: "Item", render: (i) => i.itemCode },
              { header: "Description", render: (i) => i.description ?? "—" },
              { header: "Qty", align: "right", render: (i) => i.quantity },
              { header: "Unit", render: (i) => i.unitCode },
            ]}
          />
        )}
      </Async>

      <h2 style={h2}>Participants</h2>
      <Async
        loading={participants.isLoading}
        error={participants.error}
        data={participants.data}
      >
        {(rows) => (
          <DataTable<Participant>
            rows={rows}
            keyOf={(p) => p.id}
            empty="No suppliers invited."
            columns={[
              { header: "Supplier", render: (p) => p.supplierCode },
              { header: "Status", render: (p) => <StatusPill status={p.status} /> },
              { header: "Invited", render: (p) => new Date(p.invitedAt).toLocaleDateString() },
            ]}
          />
        )}
      </Async>
    </section>
  );
}

const dt = { color: "#64748b", fontSize: 13 } as const;
const dd = { margin: 0, fontSize: 14 } as const;
const h2 = { fontSize: 16, marginTop: 28 } as const;
