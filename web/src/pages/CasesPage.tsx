import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { casesApi } from '../api';
import type { Case, CaseOption, CaseStatus, CaseType } from '../api/cases';
import { Attributes, claimValueText, Validity } from '../components/ClaimBits';
import { EvidenceCard } from '../components/Evidence';
import { Empty, ErrorBox, Loading, Missing } from '../components/ui';
import { dueUrgency, errorMessage, formatDate, formatDateTime } from '../lib/format';
import { caseTypeLabel, sourceLabel, subjectLabel } from '../lib/labels';
import { useAsync } from '../lib/useAsync';
import { useSession } from '../session';

/** Grupos de la bandeja: cada píldora reúne varios tipos de caso. */
type Group = 'swap' | 'expiry' | 'mismatch' | 'other';

const GROUP_OF: Record<CaseType, Group> = {
  SWAP_INCOMPLETE: 'swap',
  CONTRACT_EXPIRING: 'expiry',
  TEMP_SUPPORT_EXPIRED: 'expiry',
  BUDGET_ASSIGNMENT_MISMATCH: 'mismatch',
  MOVEMENT_NOT_REFLECTED: 'mismatch',
  MANAGER_MISMATCH: 'mismatch',
  MOVEMENT_WITHOUT_EVIDENCE: 'other',
  AVAILABILITY_UNCONFIRMED: 'other',
  INVENTORY_MISMATCH: 'mismatch',
  UNMATCHED_IDENTITY: 'other',
};

const GROUPS: { key: Group; label: string; tone: string }[] = [
  { key: 'swap', label: 'Enroque', tone: 'tone-primary' },
  { key: 'expiry', label: 'Vencimiento', tone: 'tone-warn' },
  { key: 'mismatch', label: 'Desfase', tone: 'tone-neutral' },
  { key: 'other', label: 'Otros', tone: 'tone-danger' },
];

const groupOf = (type: CaseType): Group => GROUP_OF[type] ?? 'other';
const toneOf = (type: CaseType): string => GROUPS.find((g) => g.key === groupOf(type))?.tone ?? 'tone-neutral';

function dueText(kase: Case): string {
  if (kase.status === 'RESOLVED') return 'resuelto';
  if (!kase.dueDate) return 'sin fecha límite';
  const overdue = dueUrgency(kase.dueDate).urgency === 'overdue';
  return `${overdue ? 'venció el' : 'responder antes del'} ${formatDate(kase.dueDate)}`;
}

function CaseRow({ kase, selected }: { kase: Case; selected: boolean }) {
  return (
    <li>
      <Link to={`/cases/${encodeURIComponent(kase.id)}`} className={`case-row${selected ? ' selected' : ''}`}>
        <div className={`case-type ${toneOf(kase.type)}`}>{caseTypeLabel(kase.type)}</div>
        <div className="case-row-title">{kase.personName ?? kase.personId ?? <Missing>Persona no informada</Missing>}</div>
        <div className="case-row-meta">
          {kase.title ?? 'Caso sin título'} · {dueText(kase)}
        </div>
      </Link>
    </li>
  );
}

export function CasesPage() {
  const { caseId } = useParams();
  const { user } = useSession();
  const [status, setStatus] = useState<CaseStatus | ''>('OPEN');
  const [group, setGroup] = useState<Group | ''>('');

  const list = useAsync(() => casesApi.listCases({ status: status || undefined }), [status]);

  const all = [...(list.data ?? [])].sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'));
  const cases = group ? all.filter((c) => groupOf(c.type) === group) : all;
  const count = (g: Group) => all.filter((c) => groupOf(c.type) === g).length;

  return (
    <div className="split">
      <section className="inbox" aria-label="Lista de casos">
        <h1>
          Bandeja de casos
        </h1>
        <div className="pills" role="group" aria-label="Filtrar por tipo de caso">
          <button type="button" className="pill" aria-pressed={group === ''} onClick={() => setGroup('')}>
            Todos {list.data ? all.length : ''}
          </button>
          {GROUPS.map((g) => (
            <button key={g.key} type="button" className="pill" aria-pressed={group === g.key} onClick={() => setGroup(g.key)}>
              {g.label} {list.data ? count(g.key) : ''}
            </button>
          ))}
        </div>
        <div className="inbox-tools">
          <span>Casos a cargo de {user.name}</span>
          <label>
            Estado
            <select value={status} onChange={(e) => setStatus(e.target.value as CaseStatus | '')}>
              <option value="OPEN">Abiertos</option>
              <option value="RESOLVED">Resueltos</option>
              <option value="">Todos</option>
            </select>
          </label>
        </div>
        {list.error ? <ErrorBox message={list.error} onRetry={list.reload} /> : null}
        {!list.data && list.loading ? <Loading label="Cargando casos…" /> : null}
        {list.data && cases.length === 0 ? (
          <Empty title="No hay casos con estos filtros.">
            Solo se muestran los casos de los que este usuario es responsable, o de alguien a su cargo.
          </Empty>
        ) : null}
        <ul className="case-list">
          {cases.map((c) => (
            <CaseRow key={c.id} kase={c} selected={c.id === caseId} />
          ))}
        </ul>
      </section>

      <section className="panel" aria-label="Detalle del caso">
        {caseId ? (
          <CaseDetail
            key={`${user.id}:${caseId}`}
            caseId={caseId}
            fromList={list.data?.find((c) => c.id === caseId)}
            listLoading={list.loading && !list.data}
            onDecided={list.reload}
          />
        ) : (
          <Empty title="Selecciona un caso de la lista.">
            Verás lo que dice cada fuente, la evidencia y las opciones para decidir.
          </Empty>
        )}
      </section>
    </div>
  );
}

function CaseDetail({
  caseId,
  fromList,
  listLoading,
  onDecided,
}: {
  caseId: string;
  fromList: Case | undefined;
  listLoading: boolean;
  onDecided: () => void;
}) {
  const { user } = useSession();
  const [decided, setDecided] = useState<Case | null>(null);
  const fetched = useAsync(() => casesApi.getCase(caseId), [caseId], !decided && !fromList && !listLoading);
  const kase = decided ?? fromList ?? fetched.data;

  const [pending, setPending] = useState<CaseOption | null>(null);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (!kase) {
    if (fetched.error) return <ErrorBox message={fetched.error} onRetry={fetched.reload} />;
    return <Loading label="Cargando el caso…" />;
  }

  async function confirm() {
    if (!pending || !kase) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await casesApi.decide(kase.id, { optionId: pending.id, comment: comment.trim() || undefined });
      setDecided(result);
      setPending(null);
      setComment('');
      onDecided();
    } catch (e) {
      setSubmitError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  const claims = kase.claims ?? [];
  const evidence = kase.evidence ?? [];
  const options = kase.options ?? [];
  const summary = kase.summary;
  const backup = summary?.mode === 'RECORDED' || summary?.mode === 'FALLBACK';
  const decision = kase.decision;

  const open = kase.status === 'OPEN';

  return (
    <article className="case-detail">
      <header className="case-head">
        <div className={`eyebrow ${toneOf(kase.type)}`}>
          {caseTypeLabel(kase.type)} · {open ? 'abierto' : 'resuelto'}
          {kase.openedAt ? ` · abierto el ${formatDate(kase.openedAt)}` : ''}
        </div>
        <h2>{kase.title ?? <Missing>Caso sin título</Missing>}</h2>
        <p className="muted">
          {kase.personId ? (
            <Link to={`/people?id=${encodeURIComponent(kase.personId)}`}>{kase.personName ?? kase.personId}</Link>
          ) : (
            'Persona no informada'
          )}{' '}
          · Responsable: {kase.ownerName ?? kase.ownerId ?? 'no informado'} · {dueText(kase)}
        </p>
      </header>

      {!open ? (
        <section className="decision-box">
          <h3>Decisión registrada</h3>
          {decision ? (
            <>
              <p className="decision-label">{decision.optionLabel ?? decision.optionId ?? <Missing>Opción no informada</Missing>}</p>
              <p>
                {decision.decidedBy ? `Decidió ${decision.decidedBy}` : 'No se informó quién decidió'}
                {decision.decidedAt ? ` el ${formatDateTime(decision.decidedAt)}` : ''}.
                {decision.comment ? ` Comentario: ${decision.comment}` : ''}
              </p>
              {decision.ledgerClaimIds && decision.ledgerClaimIds.length > 0 ? (
                <p className="muted small">Registro en el libro: {decision.ledgerClaimIds.join(', ')}</p>
              ) : null}
            </>
          ) : (
            <p>
              <Missing>El caso figura como resuelto, pero el servicio no informó la decisión.</Missing>
            </p>
          )}
        </section>
      ) : null}

      <section className={`ai-box${summary?.aiGenerated ? '' : ' plain'}`}>
        <div className="ai-box-label">
          {summary?.aiGenerated ? 'Resumen redactado por IA' : backup ? 'Resumen de plantilla, sin modelo de IA' : 'Resumen'}
        </div>
        {summary?.text ? <p>{summary.text}</p> : <Missing>Este caso no tiene resumen.</Missing>}
      </section>

      <section>
        <h3>Lo que dice cada fuente</h3>
        {claims.length === 0 ? (
          <Missing>El caso no trae afirmaciones.</Missing>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Dato</th>
                  <th>Valor</th>
                  <th>Fuente y fecha</th>
                </tr>
              </thead>
              <tbody>
                {claims.map((c, i) => (
                  <tr key={c.claimId ?? i}>
                    <td>
                      <strong>{subjectLabel(c.subject)}</strong>
                    </td>
                    <td>
                      <strong>{claimValueText(c.subject, c.value) ?? <Missing>Sin valor</Missing>}</strong>
                      <Attributes attributes={c.attributes} />
                      <Validity from={c.validFrom} until={c.validUntil} />
                    </td>
                    <td>
                      {sourceLabel(c.source) ?? <Missing>Fuente no informada</Missing>}
                      {c.sourceRef ? <span className="muted"> · {c.sourceRef}</span> : null}
                      <div className="sub">
                        {c.observedAt ? `consultado el ${formatDate(c.observedAt)}` : 'fecha de consulta no informada'}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section>
        <h3>Evidencia</h3>
        {evidence.length === 0 ? (
          <div className="state state-empty compact">No se encontró evidencia (correo de aprobación o conformidad) para este caso.</div>
        ) : (
          evidence.map((e, i) => <EvidenceCard key={`${e.emailId ?? 'ev'}-${i}`} evidence={e} />)
        )}
      </section>

      <section>
        <h3>{open ? 'Decisión' : 'Opciones que tenía el caso'}</h3>
        {options.length === 0 ? (
          <Missing>El caso no trae opciones; no es posible decidir desde aquí.</Missing>
        ) : open ? (
          <>
            <p className="decision-hint">La decisión queda registrada con tu nombre y la fecha.</p>
            <div className="decision-buttons">
              {options.map((o, i) => (
                <button
                  key={o.id}
                  type="button"
                  className={`btn${i === 0 ? ' btn-primary' : ''}`}
                  aria-pressed={pending?.id === o.id}
                  disabled={submitting}
                  onClick={() => {
                    setPending(o);
                    setSubmitError(null);
                  }}
                >
                  {o.label ?? o.id}
                </button>
              ))}
            </div>
          </>
        ) : (
          <ul className="option-list">
            {options.map((o) => (
              <li key={o.id} className={decision?.optionId === o.id ? 'chosen' : undefined}>
                {o.label ?? o.id}
                {decision?.optionId === o.id ? ' (elegida)' : ''}
              </li>
            ))}
          </ul>
        )}

        {pending && open ? (
          <div className="confirm-box">
            <p>
              Vas a decidir <strong>«{pending.label ?? pending.id}»</strong>. Se registrará a nombre de {user.name} y el caso quedará
              resuelto.
            </p>
            {pending.text ? <p className="muted">{pending.text}</p> : null}
            <label>
              Comentario (opcional)
              <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={2} maxLength={500} />
            </label>
            {submitError ? <p className="text-danger">{submitError}</p> : null}
            <div className="row-actions">
              <button type="button" className="btn btn-primary" onClick={confirm} disabled={submitting}>
                {submitting ? 'Registrando…' : 'Confirmar decisión'}
              </button>
              <button type="button" className="btn" onClick={() => setPending(null)} disabled={submitting}>
                Cancelar
              </button>
            </div>
          </div>
        ) : null}
      </section>
    </article>
  );
}
