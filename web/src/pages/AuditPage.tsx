import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ledgerApi } from '../api';
import type { AuditType } from '../api/ledger';
import { EvidenceCard } from '../components/Evidence';
import { Chip, Empty, ErrorBox, Loading, Missing, PageHeader } from '../components/ui';
import { formatDate } from '../lib/format';
import { auditTypeLabel } from '../lib/labels';
import { useAsync } from '../lib/useAsync';

export function AuditPage() {
  const [person, setPerson] = useState('');
  const [type, setType] = useState<AuditType | ''>('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  // Se carga todo una vez y se filtra aquí, al instante, mientras la persona escribe o elige.
  const audit = useAsync(() => ledgerApi.listAudit(), []);
  const invalidRange = !!from && !!to && from > to;
  const q = person.trim().toLowerCase();
  const entries = (audit.data ?? []).filter((e) => {
    const day = (e.date ?? '').slice(0, 10);
    return (
      (!q || `${e.personId ?? ''} ${e.personName ?? ''}`.toLowerCase().includes(q)) &&
      (!type || e.type === type) &&
      (invalidRange || ((!from || day >= from) && (!to || day <= to)))
    );
  });
  const missing = entries.filter((e) => e.missingEvidence).length;
  const filtering = !!q || !!type || !!from || !!to;

  function clear() {
    setPerson('');
    setType('');
    setFrom('');
    setTo('');
  }

  return (
    <>
      <PageHeader
        title="Auditoría"
        subtitle="Cada decisión y cada movimiento con su responsable, su fecha y su evidencia. La revisión es posterior y no frena el proceso."
      />
      <div className="stack">
        <form className="filters-inline" onSubmit={(e) => e.preventDefault()}>
          <label className="grow">
            Persona
            <input value={person} onChange={(e) => setPerson(e.target.value)} placeholder="Matrícula o nombre" />
          </label>
          <label>
            Tipo
            <select value={type} onChange={(e) => setType(e.target.value as AuditType | '')}>
              <option value="">Todos</option>
              <option value="DECISION">Decisiones</option>
              <option value="MOVEMENT">Movimientos</option>
              <option value="EVIDENCE">Correos de evidencia</option>
            </select>
          </label>
          <label>
            Desde
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </label>
          <label>
            Hasta
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </label>
          <button type="button" className="btn" onClick={clear} disabled={!filtering}>
            Limpiar
          </button>
          {audit.data ? (
            <span className="muted small">
              {entries.length} de {audit.data.length} registros
            </span>
          ) : null}
          {invalidRange ? <span className="text-danger small">«Desde» no puede ser posterior a «Hasta».</span> : null}
        </form>

        {missing > 0 ? (
          <div className="banner-danger" role="status">
            {missing === 1
              ? '1 hallazgo: un movimiento figura sin evidencia.'
              : `${missing} hallazgos: movimientos que figuran sin evidencia.`}
          </div>
        ) : null}

        <section className="panel">
          {audit.error ? <ErrorBox message={audit.error} onRetry={audit.reload} /> : null}
          {audit.loading && !audit.data ? <Loading label="Cargando auditoría…" /> : null}
          {audit.data && entries.length === 0 ? <Empty title="No hay entradas de auditoría con estos filtros." /> : null}
          {entries.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Qué pasó</th>
                    <th>Persona</th>
                    <th>Quién decidió o aprobó</th>
                    <th>Evidencia</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((e) => {
                    const evidence = e.evidence ?? [];
                    return (
                      <tr key={e.id} className={e.type === 'DECISION' ? 'row-decision' : undefined}>
                        <td className="nowrap">{formatDate(e.date) ?? <Missing>Sin fecha</Missing>}</td>
                        <td>
                          <strong>{auditTypeLabel(e.type)}</strong>
                          <div className="sub">{e.description ?? 'Sin descripción'}</div>
                        </td>
                        <td className="nowrap">
                          {e.personId ? (
                            <Link to={`/people?id=${encodeURIComponent(e.personId)}`}>{e.personName ?? e.personId}</Link>
                          ) : (
                            <Missing>No informada</Missing>
                          )}
                          <div className="sub">{e.personId}</div>
                        </td>
                        <td>{e.responsible || 'Sin registro'}</td>
                        <td>
                          {evidence.map((ev, i) => (
                            <EvidenceCard key={i} evidence={ev} compact />
                          ))}
                          {evidence.length === 0 ? (
                            e.missingEvidence ? (
                              'No se encontró'
                            ) : e.type === 'DECISION' ? (
                              <span className="muted">Decisión tomada en la bandeja de casos</span>
                            ) : (
                              <span className="muted">Sin evidencia adjunta</span>
                            )
                          ) : null}
                        </td>
                        <td>
                          {e.missingEvidence ? (
                            <Chip tone="danger">Hallazgo: sin evidencia</Chip>
                          ) : evidence.length > 0 ? (
                            <Chip>Con evidencia</Chip>
                          ) : e.type === 'DECISION' ? (
                            <Chip tone="solid">Decisión registrada</Chip>
                          ) : (
                            <Chip>Registrada</Chip>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>
    </>
  );
}
