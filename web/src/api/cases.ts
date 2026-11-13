// Tipos derivados a mano de contracts/cases.yaml
import { request } from './http';

export type CaseStatus = 'OPEN' | 'RESOLVED';

export const CASE_TYPES = [
  'BUDGET_ASSIGNMENT_MISMATCH',
  'MOVEMENT_NOT_REFLECTED',
  'MANAGER_MISMATCH',
  'MOVEMENT_WITHOUT_EVIDENCE',
  'SWAP_INCOMPLETE',
  'CONTRACT_EXPIRING',
  'AVAILABILITY_UNCONFIRMED',
  'TEMP_SUPPORT_EXPIRED',
  'INVENTORY_MISMATCH',
  'UNMATCHED_IDENTITY',
] as const;
export type CaseType = (typeof CASE_TYPES)[number];

export interface ClaimSnapshot {
  claimId?: string;
  subject?: string;
  value?: string;
  attributes?: Record<string, string>;
  source?: string;
  sourceRef?: string;
  observedAt?: string;
  validFrom?: string;
  validUntil?: string;
}

export interface CaseEvidence {
  type?: string;
  emailId?: string;
  quote?: string;
  approver?: string;
  personIds?: string[];
  date?: string;
  aiGenerated?: boolean;
}

export interface CaseOption {
  id: string;
  label?: string;
  text?: string;
}

export interface CaseSummary {
  text?: string;
  aiGenerated?: boolean;
  mode?: 'LIVE' | 'RECORDED' | 'FALLBACK';
}

export interface CaseDecision {
  optionId?: string;
  optionLabel?: string;
  comment?: string;
  decidedBy?: string;
  decidedAt?: string;
  ledgerClaimIds?: string[];
}

export interface Case {
  id: string;
  type: CaseType;
  status: CaseStatus;
  title?: string;
  personId?: string;
  personName?: string;
  ownerId?: string;
  ownerName?: string;
  dueDate?: string;
  openedAt?: string;
  claims?: ClaimSnapshot[];
  evidence?: CaseEvidence[];
  summary?: CaseSummary | null;
  options?: CaseOption[];
  decision?: CaseDecision | null;
}

export interface AvailabilityItem {
  personId?: string;
  personName?: string;
  personRole?: string;
  availableFrom?: string;
  status?: 'PLANNED' | 'CONFIRMED';
  source?: string;
  observedAt?: string;
  confirmerId?: string;
  confirmerName?: string;
  alert?: boolean;
  caseId?: string | null;
}

export interface AlertSettings {
  contractExpiryDays: number;
  availabilityDays: number;
  updatedAt?: string;
  updatedBy?: string;
}

export interface CaseFilters {
  status?: CaseStatus;
  type?: CaseType;
  personId?: string;
}

export interface DecisionInput {
  optionId: string;
  comment?: string;
}

export interface CasesApi {
  listCases(filters?: CaseFilters): Promise<Case[]>;
  getCase(id: string): Promise<Case>;
  decide(id: string, input: DecisionInput): Promise<Case>;
  listAvailability(): Promise<AvailabilityItem[]>;
  getAlertSettings(): Promise<AlertSettings>;
  putAlertSettings(settings: Pick<AlertSettings, 'contractExpiryDays' | 'availabilityDays'>): Promise<AlertSettings>;
}

export const DECISION_ERRORS: Record<number, string> = {
  400: 'La opción elegida no es válida o falta el usuario.',
  403: 'El usuario simulado no es responsable de esta persona, por lo que no puede decidir este caso.',
  404: 'El caso no existe.',
  409: 'El caso ya fue resuelto por otra persona.',
  502: 'No se pudo registrar la decisión en el libro; el caso sigue abierto.',
};

export const SETTINGS_ERRORS: Record<number, string> = {
  400: 'Valores fuera de rango: los días deben estar entre 1 y 365.',
};

export const httpCasesApi: CasesApi = {
  listCases: (filters = {}) =>
    request<Case[]>('cases', '/cases', {
      query: { status: filters.status, type: filters.type, personId: filters.personId },
    }),
  getCase: (id) =>
    request<Case>('cases', `/cases/${encodeURIComponent(id)}`, { statusMessages: { 404: 'El caso no existe.' } }),
  decide: (id, input) =>
    request<Case>('cases', `/cases/${encodeURIComponent(id)}/decisions`, {
      method: 'POST',
      body: input,
      statusMessages: DECISION_ERRORS,
    }),
  listAvailability: () => request<AvailabilityItem[]>('cases', '/availability'),
  getAlertSettings: () => request<AlertSettings>('cases', '/alert-settings'),
  putAlertSettings: (settings) =>
    request<AlertSettings>('cases', '/alert-settings', { method: 'PUT', body: settings, statusMessages: SETTINGS_ERRORS }),
};
