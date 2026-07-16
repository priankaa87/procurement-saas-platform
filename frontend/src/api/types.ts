// Shapes returned by the gateway, mirrored from the services' DTOs. Kept deliberately
// narrow — only the fields the UI actually uses — so a change on either side that matters
// shows up as a type error rather than a runtime surprise.

export interface Tender {
  id: number;
  code: string;
  title: string;
  status: string;
  currencyCode: string;
  bidDeadline: string;
  awardedSupplierCode: string | null;
  itemCount: number;
}

export interface TenderItem {
  id: number;
  itemCode: string;
  description: string | null;
  quantity: number;
  unitCode: string;
}

export interface Participant {
  id: number;
  supplierCode: string;
  status: string;
  invitedAt: string;
}

export interface Supplier {
  id: number;
  code: string;
  name: string;
  status: string;
  countryIso2: string | null;
  categoryCodes: string[];
}

export interface ReportDefinition {
  id: number;
  code: string;
  name: string;
  description: string | null;
  format: string;
  active: boolean;
}

export interface ReportJob {
  id: number;
  definitionCode: string;
  status: string;
  rowCount: number | null;
  error: string | null;
  createdAt: string;
  downloadable: boolean;
}
