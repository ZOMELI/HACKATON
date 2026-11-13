import { formatDate } from '../lib/format';
import { evidenceTypeLabel } from '../lib/labels';
import { AiNote, Missing } from './ui';

export interface EvidenceLike {
  type?: string;
  emailId?: string;
  quote?: string;
  approver?: string;
  personIds?: string[];
  date?: string;
  aiGenerated?: boolean;
}

/** Evidencia de un correo. `compact` la reduce a dos líneas para tablas y la línea de tiempo. */
export function EvidenceCard({ evidence, compact = false }: { evidence: EvidenceLike; compact?: boolean }) {
  const date = formatDate(evidence.date);
  if (compact) {
    return (
      <div className="evidence compact">
        <div>
          {date ? `Correo del ${date}` : 'Correo sin fecha'}
          {evidence.emailId ? <span className="muted"> · {evidence.emailId}</span> : null}
        </div>
        {evidence.quote ? <div className="muted small">«{evidence.quote}»</div> : null}
        {evidence.aiGenerated ? <AiNote /> : null}
      </div>
    );
  }
  return (
    <div className="evidence">
      <strong>Correo: {evidenceTypeLabel(evidence.type)}</strong>
      <div className="muted">
        {date ?? 'Sin fecha'} · {evidence.approver ? `de ${evidence.approver}` : 'remitente no informado'}
        {evidence.emailId ? ` · ${evidence.emailId}` : ''}
      </div>
      <div className="evidence-quote">{evidence.quote ? `«${evidence.quote}»` : <Missing>Sin cita textual</Missing>}</div>
      {evidence.aiGenerated ? <AiNote /> : null}
    </div>
  );
}
