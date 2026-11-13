import type { CaseType } from '../api/cases';

function lookup(map: Record<string, string>, key: string | undefined | null): string | null {
  if (!key) return null;
  return map[key] ?? key;
}

export const CASE_TYPE_LABELS: Record<CaseType, string> = {
  BUDGET_ASSIGNMENT_MISMATCH: 'Presupuesto y asignación no coinciden',
  MOVEMENT_NOT_REFLECTED: 'Movimiento no reflejado en SIGA',
  MANAGER_MISMATCH: 'Responsable distinto entre fuentes',
  MOVEMENT_WITHOUT_EVIDENCE: 'Movimiento sin evidencia de aprobación',
  SWAP_INCOMPLETE: 'Enroque con conformidad incompleta',
  CONTRACT_EXPIRING: 'Acta por vencer',
  AVAILABILITY_UNCONFIRMED: 'Disponibilidad sin confirmar',
  TEMP_SUPPORT_EXPIRED: 'Apoyo temporal vencido',
  INVENTORY_MISMATCH: 'Desfase de inventario',
  UNMATCHED_IDENTITY: 'Sin correspondencia',
};
export const caseTypeLabel = (t: string | undefined) => lookup(CASE_TYPE_LABELS, t) ?? 'Tipo sin dato';

const CASE_STATUS: Record<string, string> = { OPEN: 'Abierto', RESOLVED: 'Resuelto' };
export const caseStatusLabel = (s: string | undefined) => lookup(CASE_STATUS, s) ?? 'Sin estado';

const SUBJECTS: Record<string, string> = {
  ORG_MEMBERSHIP: 'Pertenencia organizacional',
  MANAGEMENT_RESPONSIBILITY: 'Responsable de gestión',
  BUDGET_ORIGIN: 'Origen presupuestal',
  OPERATIONAL_ASSIGNMENT: 'Asignación operativa',
  AVAILABILITY: 'Disponibilidad',
  CONTRACT: 'Acta / contrato',
  MOVEMENT: 'Movimiento',
  TEMP_SUPPORT: 'Apoyo temporal',
  DECISION: 'Decisión',
  ROLE: 'Rol',
  EVIDENCE: 'Evidencia de correo',
};
export const subjectLabel = (s: string | undefined) => lookup(SUBJECTS, s) ?? 'Dato sin clasificar';

const SOURCES: Record<string, string> = {
  SIGA: 'SIGA',
  PREPER: 'PREPER',
  JIRA: 'Jira',
  EXCEL_CHAPTER: 'Excel de chapters',
  EXCEL_MOVEMENTS: 'Excel de movimientos',
  EXCEL_CONTRACTS: 'Excel de actas',
  EXCEL_SUPPORT: 'Excel de apoyos temporales',
  EXCEL_RECRUITMENT: 'Excel de Reclutamiento',
  EMAIL: 'Correo',
  DECISION: 'Decisión del responsable',
  CASE_DECISION: 'Decisión del responsable',
  INVENTORY: 'Valida tu equipo',
  PROVIDER_ACT: 'Actas de proveedores',
  MOVIPER: 'MoviPer',
  STAFFING_PORTAL: 'Portal de staffing',
  WEB: 'Registro en la web',
};
export const sourceLabel = (s: string | undefined | null) => lookup(SOURCES, s);

const ATTRIBUTES: Record<string, string> = {
  squad: 'Squad',
  chapter: 'Chapter',
  unitName: 'Unidad',
  company: 'Empresa',
  unmatched: 'Sin persona enlazada',
  category: 'Categoría',
  project: 'Proyecto',
  key: 'Proyecto Jira',
  ref: 'Caja',
  unit: 'Unidad',
  tribe: 'Tribu',
  managerName: 'Nombre del responsable',
  type: 'Tipo',
  fromTribe: 'Desde',
  toTribe: 'Hacia',
  status: 'Estado',
  counterpartId: 'Contraparte',
  provider: 'Proveedor',
  reason: 'Motivo',
  caseId: 'Caso',
  caseType: 'Tipo de caso',
  optionLabel: 'Opción elegida',
  comment: 'Comentario',
};
export const attributeLabel = (k: string) => ATTRIBUTES[k] ?? k;

const ATTRIBUTE_VALUES: Record<string, string> = {
  MOVE: 'Traslado',
  SWAP: 'Enroque',
  APPROVED: 'Aprobado',
  REQUESTED: 'Solicitado',
  PLANNED: 'Prevista',
  CONFIRMED: 'Confirmada',
  ...CASE_TYPE_LABELS,
};
export const valueLabel = (v: string | undefined | null) => lookup(ATTRIBUTE_VALUES, v);

const EVIDENCE_TYPES: Record<string, string> = {
  MOVEMENT_APPROVAL: 'Aprobación de movimiento',
  SWAP_CONFORMITY: 'Conformidad de enroque',
  CONTRACT_RENEWAL: 'Renovación de acta',
  CONTRACT_REPLACEMENT: 'Reemplazo de acta',
  TEMP_SUPPORT: 'Apoyo temporal',
  INFORMAL_NEED: 'Necesidad informal',
};
export const evidenceTypeLabel = (t: string | undefined) => lookup(EVIDENCE_TYPES, t) ?? 'Evidencia';

const AVAILABILITY: Record<string, string> = { PLANNED: 'Prevista', CONFIRMED: 'Confirmada' };
export const availabilityLabel = (s: string | undefined) => lookup(AVAILABILITY, s) ?? 'Sin estado';

export const MECHANISM_LABELS: Record<string, string> = {
  INTERNAL_MOVE: 'Movimiento interno',
  EXTERNAL_HIRE: 'Contratación externa',
  PROVIDER: 'Proveedor',
  TEMP_SUPPORT: 'Apoyo temporal',
  UNDEFINED: 'Por definir',
};
export const mechanismLabel = (m: string | undefined) => lookup(MECHANISM_LABELS, m);

const RECRUITMENT: Record<string, string> = {
  NOT_STARTED: 'No iniciado',
  SOURCING: 'Búsqueda de candidatos',
  INTERVIEWS: 'Entrevistas',
  OFFER: 'Oferta',
  HIRED: 'Contratado',
  CANCELLED: 'Cancelado',
};
export const recruitmentLabel = (s: string | undefined) => lookup(RECRUITMENT, s);

export const NEED_STATUS_LABELS: Record<string, string> = { OPEN: 'Abierta', COVERED: 'Cubierta', CANCELLED: 'Cancelada' };
export const needStatusLabel = (s: string | undefined) => lookup(NEED_STATUS_LABELS, s) ?? 'Sin estado';

const AUDIT_TYPES: Record<string, string> = { DECISION: 'Decisión', MOVEMENT: 'Movimiento', EVIDENCE: 'Correo de evidencia' };
export const auditTypeLabel = (t: string | undefined) => lookup(AUDIT_TYPES, t) ?? 'Sin tipo';

const DIMENSION_STATUS: Record<string, string> = {
  CONFIRMED: 'Confirmado',
  PENDING_CONFIRMATION: 'Pendiente de confirmar',
  MISSING: 'Sin dato',
};
export const dimensionStatusLabel = (s: string | undefined) => lookup(DIMENSION_STATUS, s) ?? 'Estado no informado';
