// Tipos que usan las pantallas. El cliente HTTP de abajo los llena desde el ledger-service real.
import { request } from './http';

export type Subject =
  | 'ORG_MEMBERSHIP'
  | 'MANAGEMENT_RESPONSIBILITY'
  | 'BUDGET_ORIGIN'
  | 'OPERATIONAL_ASSIGNMENT'
  | 'AVAILABILITY'
  | 'CONTRACT'
  | 'MOVEMENT'
  | 'TEMP_SUPPORT'
  | 'DECISION'
  | 'ROLE'
  | 'EVIDENCE';

export const DIMENSIONS: Subject[] = [
  'ORG_MEMBERSHIP',
  'MANAGEMENT_RESPONSIBILITY',
  'BUDGET_ORIGIN',
  'OPERATIONAL_ASSIGNMENT',
];

export type Source =
  | 'SIGA'
  | 'PREPER'
  | 'JIRA'
  | 'EXCEL_CHAPTER'
  | 'EXCEL_MOVEMENTS'
  | 'EXCEL_CONTRACTS'
  | 'EXCEL_SUPPORT'
  | 'EXCEL_RECRUITMENT'
  | 'EMAIL'
  | 'DECISION';

export interface LedgerEvidence {
  type: string;
  quote: string;
  emailId?: string;
  approver?: string;
  personIds?: string[];
  date?: string;
  aiGenerated?: boolean;
}

export interface Claim {
  id: string;
  recordedAt: string;
  personId: string;
  personName?: string;
  personRole?: string;
  subject: Subject;
  value: string;
  attributes?: Record<string, string>;
  source: Source;
  sourceRef?: string;
  observedAt: string;
  validFrom?: string;
  validUntil?: string;
  supersedes?: string;
  evidence?: LedgerEvidence[];
  recordedBy?: string;
}

export interface PersonRef {
  personId: string;
  name?: string;
  email?: string;
  role?: string;
}

export type DimensionStatus = 'CONFIRMED' | 'PENDING_CONFIRMATION' | 'MISSING';

export interface DimensionView {
  subject: Subject;
  value?: string;
  displayValue?: string;
  source?: Source;
  observedAt?: string;
  status?: DimensionStatus;
  alternatives?: Claim[];
}

export interface SalaryGrade {
  visible?: boolean;
  value?: string | null;
  source?: string;
  observedAt?: string;
}

export interface Profile {
  person?: PersonRef;
  dimensions?: DimensionView[];
  salaryGrade?: SalaryGrade;
}

export type AuditType = 'DECISION' | 'MOVEMENT' | 'EVIDENCE';

export interface AuditEntry {
  id: string;
  type?: AuditType;
  personId?: string;
  personName?: string;
  description?: string;
  responsible?: string;
  date?: string;
  evidence?: LedgerEvidence[];
  missingEvidence?: boolean;
}

export interface AuditFilters {
  personId?: string;
  type?: AuditType;
  from?: string;
  to?: string;
}

export interface LedgerApi {
  searchPeople(query: string): Promise<PersonRef[]>;
  getProfile(personId: string): Promise<Profile>;
  getHistory(personId: string): Promise<Claim[]>;
  listAudit(filters?: AuditFilters): Promise<AuditEntry[]>;
}

const NOT_FOUND = { 404: 'No se encontró a la persona en el libro.' };

// ---- Adaptación al ledger-service real (services/ledger-service, openapi.yml) ----
// El servicio publica un modelo plano: `dimension`, y `value` codificado como "clave=valor;...".
// Aquí se traduce al modelo que usan las pantallas.

interface RawClaim {
  id: number;
  personId: string;
  personName?: string | null;
  personEmail?: string | null;
  dimension: string;
  value: string;
  source: string;
  validFrom?: string | null;
  validTo?: string | null;
  observedAt?: string | null;
  status?: string;
  evidenceRef?: string | null;
  author?: string | null;
  recordedAt: string;
}

interface RawProfile {
  personId: string;
  fullName?: string | null;
  email?: string | null;
  dimensions?: { dimension: string; state: string; claims: RawClaim[] }[];
}

const SUBJECT_BY_DIMENSION: Record<string, Subject> = {
  ORG_MEMBERSHIP: 'ORG_MEMBERSHIP',
  MANAGEMENT: 'MANAGEMENT_RESPONSIBILITY',
  FUNDING: 'BUDGET_ORIGIN',
  OPERATIONAL_ASSIGNMENT: 'OPERATIONAL_ASSIGNMENT',
  AVAILABILITY: 'AVAILABILITY',
  DECISION: 'DECISION',
};

const STATUS_BY_STATE: Record<string, DimensionStatus> = {
  CONSISTENT: 'CONFIRMED',
  DISCREPANCY: 'PENDING_CONFIRMATION',
  PENDING: 'MISSING',
};

function decode(value: string): Record<string, string> {
  const out: Record<string, string> = {};
  if (!value || !value.includes('=')) return out;
  for (const part of value.split(';')) {
    const eq = part.indexOf('=');
    if (eq > 0) out[part.slice(0, eq).trim()] = part.slice(eq + 1).trim();
  }
  return out;
}

/** Texto legible de un valor codificado, según la dimensión. */
function display(dimension: string, value: string): string {
  const a = decode(value);
  const join = (...parts: (string | undefined)[]) => parts.filter(Boolean).join(' · ');
  switch (dimension) {
    case 'ORG_MEMBERSHIP':
      return join(a.tribe ? `Tribu ${a.tribe}` : undefined, a.unit ? `Unidad ${a.unit}` : a.unitName, a.squad ? `Squad ${a.squad}` : undefined) || value;
    case 'MANAGEMENT':
      return join(a.managerName, a.manager) || value;
    case 'FUNDING':
      return join(a.ref ? `${a.ref.startsWith('ACT') ? 'Acta' : 'Caja'} ${a.ref}` : undefined, a.category, a.tribe, a.type ?? a.provider) || value;
    case 'OPERATIONAL_ASSIGNMENT':
      return join(a.squad ? `Squad ${a.squad}` : undefined, a.category, a.project) || value;
    case 'EVIDENCE':
      return a.ref
        ? join(a.ref, a.kind, a.from && a.from !== 'CONFIDENCIAL' ? `de ${a.from} a ${a.to}` : undefined, a.status, a.evidence ? undefined : 'sin evidencia')
        : join(a.subject, a.quote ? `«${a.quote}»` : undefined) || value;
    case 'FUNDING_ACT':
    default:
      return a.value ?? value;
  }
}

function toClaim(c: RawClaim): Claim {
  return {
    id: String(c.id),
    recordedAt: c.recordedAt,
    personId: c.personId,
    personName: c.personName ?? undefined,
    subject: (c.dimension === 'EVIDENCE' ? (c.source === 'MOVIPER' ? 'MOVEMENT' : 'EVIDENCE') : (SUBJECT_BY_DIMENSION[c.dimension] ?? c.dimension)) as Subject,
    value: display(c.dimension, c.value),
    source: (c.source === 'CASE_DECISION' ? 'DECISION' : c.source) as Source,
    sourceRef: c.evidenceRef ?? undefined,
    observedAt: c.observedAt ?? c.recordedAt,
    validFrom: c.validFrom ?? undefined,
    validUntil: c.validTo ?? undefined,
    recordedBy: c.author ?? undefined,
  };
}

const listClaims = (query: Record<string, string | undefined>) => request<RawClaim[]>('ledger', '/claims', { query });

export const httpLedgerApi: LedgerApi = {
  // El servicio no tiene búsqueda de personas: se arma con la afirmación de rol de cada una (SIGA).
  async searchPeople(query) {
    const q = query.trim().toLowerCase();
    const seen = new Set<string>();
    const people: PersonRef[] = [];
    for (const c of await listClaims({ dimension: 'ROLE' })) {
      if (seen.has(c.personId)) continue;
      seen.add(c.personId);
      const person = { personId: c.personId, name: c.personName ?? undefined, email: c.personEmail ?? undefined, role: c.value };
      const text = `${person.personId} ${person.name ?? ''} ${person.email ?? ''}`.toLowerCase();
      if (!q || text.includes(q)) people.push(person);
    }
    return people.sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''));
  },

  async getProfile(personId) {
    const id = encodeURIComponent(personId);
    const [raw, roles, grade] = await Promise.all([
      request<RawProfile>('ledger', `/people/${id}/profile`, { statusMessages: NOT_FOUND }),
      listClaims({ personId, dimension: 'ROLE' }),
      // 403 = el usuario no tiene permiso para ver el grado: se muestra como oculto.
      request<{ salaryGrade?: string }>('ledger', `/people/${id}/salary-grade`).then(
        (g) => ({ visible: true, value: g.salaryGrade ?? null, source: 'SIGA' }),
        () => ({ visible: false }),
      ),
    ]);
    return {
      person: { personId: raw.personId, name: raw.fullName ?? undefined, email: raw.email ?? undefined, role: roles[0]?.value },
      dimensions: (raw.dimensions ?? []).map((d) => {
        const [first, ...rest] = d.claims;
        return {
          subject: (SUBJECT_BY_DIMENSION[d.dimension] ?? d.dimension) as Subject,
          value: first?.value,
          displayValue: first ? display(d.dimension, first.value) : undefined,
          source: first?.source as Source | undefined,
          observedAt: first?.observedAt ?? undefined,
          status: STATUS_BY_STATE[d.state] ?? 'MISSING',
          alternatives: rest.map(toClaim),
        };
      }),
      salaryGrade: grade,
    };
  },

  async getHistory(personId) {
    const raw = await request<RawClaim[]>('ledger', `/people/${encodeURIComponent(personId)}/history`, { statusMessages: NOT_FOUND });
    return raw.map((c) => (c.dimension === 'ROLE' ? { ...toClaim(c), subject: 'ROLE' as Subject } : toClaim(c)));
  },

  // El servicio solo filtra por persona; tipo y fechas se filtran aquí.
  async listAudit(filters = {}) {
    const raw = await request<RawClaim[]>('ledger', '/audit', { query: { personId: filters.personId } });
    const names = new Map<string, string>();
    for (const c of await listClaims({ dimension: 'ROLE' })) if (c.personName) names.set(c.personId, c.personName);
    // Correos indexados por el movimiento que respaldan.
    const mailByMovement = new Map<string, RawClaim>();
    for (const c of raw) {
      const a = decode(c.value);
      if (c.source === 'EMAIL' && a.movement) mailByMovement.set(a.movement, c);
    }
    const toEvidence = (c: RawClaim): LedgerEvidence => {
      const a = decode(c.value);
      return { type: a.type, quote: a.quote ?? '', emailId: a.email, approver: a.approverName ?? a.approver, date: c.observedAt ?? undefined, aiGenerated: a.ai === 'true' };
    };
    const seen = new Set<string>();
    const entries: AuditEntry[] = [];
    for (const c of raw) {
      const a = decode(c.value);
      const base = { id: String(c.id), personId: c.personId, personName: c.personName ?? names.get(c.personId) };
      if (c.dimension === 'DECISION') {
        entries.push({
          ...base,
          type: 'DECISION',
          description: [a.optionLabel ?? a.value ?? c.value, c.evidenceRef ? `Caso ${c.evidenceRef}` : '', a.comment ? `Comentario: ${a.comment}` : '']
            .filter(Boolean)
            .join(' · '),
          responsible: names.get(c.author ?? '') ?? c.author ?? undefined,
          date: c.recordedAt,
          evidence: [],
          missingEvidence: false,
        });
      } else if (c.source === 'MOVIPER') {
        if (seen.has(a.ref)) continue;
        seen.add(a.ref);
        const mail = mailByMovement.get(a.ref);
        const evidence: LedgerEvidence[] = mail
          ? [toEvidence(mail)]
          : a.evidence
            ? [{ type: 'MOVEMENT_APPROVAL', quote: `Adjunto en MoviPer: ${a.evidence}`, date: c.observedAt ?? undefined }]
            : [];
        entries.push({
          ...base,
          type: 'MOVEMENT',
          description: `${a.ref} · ${a.kind ?? 'Movimiento'}${a.from && a.from !== 'CONFIDENCIAL' ? ` · de ${a.from} a ${a.to}` : ''} · ${a.status ?? ''}`,
          responsible: mail ? decode(mail.value).approverName : a.status === 'Aprobado' ? `Requiere ${a.requiredApprover ?? 'aprobación'}` : undefined,
          date: c.validFrom ?? c.observedAt ?? c.recordedAt,
          evidence,
          missingEvidence: a.status === 'Aprobado' && evidence.length === 0,
        });
      } else if (c.source === 'EMAIL' && !a.movement) {
        entries.push({
          ...base,
          type: 'EVIDENCE',
          description: a.subject ?? a.type,
          responsible: a.approverName ?? a.approver,
          date: c.observedAt ?? c.recordedAt,
          evidence: [toEvidence(c)],
          missingEvidence: false,
        });
      }
    }
    return entries
      // Primero las decisiones (lo último que hizo una persona), luego movimientos y correos; cada grupo de lo más reciente a lo más antiguo.
      .sort((x, y) => Number(y.type === 'DECISION') - Number(x.type === 'DECISION') || (y.date ?? '').localeCompare(x.date ?? ''))
      .filter((e) => !filters.type || e.type === filters.type)
      .filter((e) => !filters.from || (e.date ?? '').slice(0, 10) >= filters.from)
      .filter((e) => !filters.to || (e.date ?? '').slice(0, 10) <= filters.to);
  },
};
