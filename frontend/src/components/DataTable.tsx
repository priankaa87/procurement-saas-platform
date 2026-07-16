import type { ReactNode } from "react";

export interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
  align?: "left" | "right";
}

interface Props<T> {
  columns: Column<T>[];
  rows: T[];
  keyOf: (row: T) => string | number;
  onRowClick?: (row: T) => void;
  empty?: string;
}

/** A plain, readable table. Deliberately unstyled beyond the essentials. */
export function DataTable<T>({ columns, rows, keyOf, onRowClick, empty }: Props<T>) {
  if (rows.length === 0) {
    return <p style={{ color: "#64748b" }}>{empty ?? "Nothing to show."}</p>;
  }
  return (
    <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}>
      <thead>
        <tr>
          {columns.map((col) => (
            <th
              key={col.header}
              style={{
                textAlign: col.align ?? "left",
                padding: "8px 12px",
                borderBottom: "2px solid #e2e8f0",
                color: "#475569",
                fontSize: 12,
                textTransform: "uppercase",
                letterSpacing: 0.4,
              }}
            >
              {col.header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr
            key={keyOf(row)}
            onClick={onRowClick ? () => onRowClick(row) : undefined}
            style={{
              cursor: onRowClick ? "pointer" : "default",
              borderBottom: "1px solid #f1f5f9",
            }}
          >
            {columns.map((col) => (
              <td
                key={col.header}
                style={{ padding: "8px 12px", textAlign: col.align ?? "left" }}
              >
                {col.render(row)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
