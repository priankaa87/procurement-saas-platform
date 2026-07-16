import { useSuppliers } from "../api/hooks";
import { Async } from "../components/Async";
import { DataTable } from "../components/DataTable";
import { StatusPill } from "../components/StatusPill";
import type { Supplier } from "../api/types";

export function SuppliersPage() {
  const { data, isLoading, error } = useSuppliers();

  return (
    <section>
      <h1 style={{ fontSize: 22 }}>Suppliers</h1>
      <Async loading={isLoading} error={error} data={data}>
        {(suppliers) => (
          <DataTable<Supplier>
            rows={suppliers}
            keyOf={(s) => s.id}
            empty="No suppliers registered."
            columns={[
              { header: "Code", render: (s) => <strong>{s.code}</strong> },
              { header: "Name", render: (s) => s.name },
              { header: "Status", render: (s) => <StatusPill status={s.status} /> },
              { header: "Country", render: (s) => s.countryIso2 ?? "—" },
              {
                header: "Categories",
                render: (s) => (s.categoryCodes.length ? s.categoryCodes.join(", ") : "—"),
              },
            ]}
          />
        )}
      </Async>
    </section>
  );
}
