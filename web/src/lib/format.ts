const DATE_FMT = new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric' });
const DATETIME_FMT = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

function parseDateOnly(value: string): Date | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (!m) return null;
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
}

/** Fecha sin hora (formato `date`). Se interpreta como día calendario, sin corrimiento de zona. */
export function formatDate(value: string | undefined | null): string | null {
  if (!value) return null;
  const d = parseDateOnly(value);
  return d && !Number.isNaN(d.getTime()) ? DATE_FMT.format(d) : value;
}

/** Fecha y hora (formato `date-time`), en la zona horaria del navegador. */
export function formatDateTime(value: string | undefined | null): string | null {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : DATETIME_FMT.format(d);
}

/** Días desde hoy hasta la fecha (negativo = ya pasó). null si no hay fecha válida. */
export function daysUntil(value: string | undefined | null): number | null {
  if (!value) return null;
  const d = parseDateOnly(value);
  if (!d || Number.isNaN(d.getTime())) return null;
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((d.getTime() - today.getTime()) / 86_400_000);
}

export function plural(n: number, one: string, many: string): string {
  return `${n} ${n === 1 ? one : many}`;
}

export type Urgency = 'overdue' | 'soon' | 'normal' | 'none';

export const SOON_DAYS = 7;

export function dueUrgency(dueDate: string | undefined | null): { urgency: Urgency; text: string } {
  const days = daysUntil(dueDate);
  if (days === null) return { urgency: 'none', text: 'Sin fecha límite' };
  if (days < 0) return { urgency: 'overdue', text: `Vencido hace ${plural(-days, 'día', 'días')}` };
  if (days === 0) return { urgency: 'soon', text: 'Vence hoy' };
  if (days <= SOON_DAYS) return { urgency: 'soon', text: `Vence en ${plural(days, 'día', 'días')}` };
  return { urgency: 'normal', text: `Vence en ${plural(days, 'día', 'días')}` };
}

export function errorMessage(error: unknown): string {
  if (error instanceof Error && error.message) return error.message;
  return 'Ocurrió un error inesperado.';
}
