import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { demandApi } from '../api';
import { MECHANISMS, type InformalNeedInput, type Mechanism, type Need, type NeedStatus } from '../api/demand';
import { Chip, Empty, ErrorBox, Loading, Missing, PageHeader } from '../components/ui';
import { errorMessage, formatDate, plural } from '../lib/format';
import { availabilityLabel, MECHANISM_LABELS, mechanismLabel, needStatusLabel, recruitmentLabel, sourceLabel } from '../lib/labels';
import { useAsync } from '../lib/useAsync';

function needName(need: Need): string {
  return need.externalId ?? (need.informal ? 'Sin registrar' : 'Sin identificador');
}

export function DemandPage() {
  const [status, setStatus] = useState<NeedStatus | ''>('OPEN');
  const [formOpen, setFormOpen] = useState(false);
  const [createdId, setCreatedId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const needs = useAsync(() => demandApi.listNeeds(status || undefined), [status]);
  const items = needs.data ?? [];
  // Sin selección explícita se muestra la primera necesidad que tenga candidatos.
  const selected =
    items.find((n) => n.id === selectedId) ?? items.find((n) => (n.candidates ?? []).length > 0) ?? items[0];

  return (
    <>
      <PageHeader
        title="Demanda"
        subtitle={`${
          needs.data ? plural(items.length, 'necesidad', 'necesidades') : 'Necesidades'
        }. El sistema propone candidatos y la decisión es del CL o del CAL.`}
      >
        <button type="button" className="btn btn-primary" onClick={() => setFormOpen((o) => !o)} aria-expanded={formOpen}>
          {formOpen ? 'Cerrar formulario' : 'Registrar necesidad informal'}
        </button>
      </PageHeader>

      <div className="stack">
        {formOpen ? (
          <section className="panel">
            <h3>Nueva necesidad informal</h3>
            <NeedForm
              onCreated={(need) => {
                setCreatedId(need.id);
                setSelectedId(need.id);
                setFormOpen(false);
                needs.reload();
              }}
            />
          </section>
        ) : null}

        <section className="panel">
          <div className="filters-inline" style={{ marginBottom: 12 }}>
            <label style={{ flexDirection: 'row', alignItems: 'center' }}>
              Estado
              <select value={status} onChange={(e) => setStatus(e.target.value as NeedStatus | '')}>
                <option value="OPEN">Abiertas</option>
                <option value="COVERED">Cubiertas</option>
                <option value="CANCELLED">Canceladas</option>
                <option value="">Todas</option>
              </select>
            </label>
            {needs.loading && needs.data ? <span className="muted small">Actualizando…</span> : null}
          </div>
          {needs.error ? <ErrorBox message={needs.error} onRetry={needs.reload} /> : null}
          {needs.loading && !needs.data ? <Loading label="Cargando necesidades…" /> : null}
          {needs.data && items.length === 0 ? <Empty title="No hay necesidades con este estado." /> : null}
          {items.length > 0 ? (
            <div className="table-wrap">
              <table className="table table-select">
                <thead>
                  <tr>
                    <th>Necesidad</th>
                    <th>Para</th>
                    <th>Perfil</th>
                    <th>Respaldo</th>
                    <th>Requerida</th>
                    <th>Reclutamiento</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((n) => (
                    <NeedRow
                      key={n.id}
                      need={n}
                      selected={n.id === selected?.id}
                      isNew={n.id === createdId}
                      onSelect={() => setSelectedId(n.id)}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>

        {selected ? <Candidates need={selected} /> : null}
      </div>
    </>
  );
}

function NeedRow({ need, selected, isNew, onSelect }: { need: Need; selected: boolean; isNew: boolean; onSelect: () => void }) {
  const rec = need.recruitment;
  return (
    <tr className={`${selected ? 'selected' : ''}${isNew ? ' need-new' : ''}`} onClick={onSelect}>
      <td>
        <button type="button" className="row-btn" aria-pressed={selected} title="Ver candidatos">
          {needName(need)}
        </button>
        {need.status && need.status !== 'OPEN' ? <div className="sub">{needStatusLabel(need.status)}</div> : null}
        {isNew ? <div className="sub">Recién registrada</div> : null}
      </td>
      <td>{[need.tribe, need.project].filter(Boolean).join(' · ') || <Missing>Sin dato</Missing>}</td>
      <td>
        {need.role ?? <Missing>Perfil no informado</Missing>}
        {need.seniority ? <div className="sub">{need.seniority}</div> : null}
      </td>
      <td>
        {need.note ? (
          need.note.includes('no existe') ? <Chip tone="danger">{need.note}</Chip> : need.note
        ) : need.mechanism === 'UNDEFINED' ? (
          'Por definir: banco o proveedor'
        ) : (
          (mechanismLabel(need.mechanism) ?? <Missing>Sin dato</Missing>)
        )}
      </td>
      <td className="nowrap">{formatDate(need.neededFrom) ?? <Missing>Sin fecha</Missing>}</td>
      <td>
        {rec ? (
          <>
            {recruitmentLabel(rec.status) ?? <Missing>Estado no informado</Missing>}
            <div className="sub">
              {sourceLabel(rec.source) ?? 'Fuente no informada'} · {formatDate(rec.observedAt) ?? 'sin fecha'}
            </div>
          </>
        ) : (
          <>
            {need.informal ? 'Necesidad informal' : <Chip tone="warn">Sin seguimiento</Chip>}
            <div className={need.source === 'EMAIL' ? 'ai-note' : 'sub'}>
              {sourceLabel(need.source) ?? 'Fuente no informada'} · {formatDate(need.observedAt) ?? 'sin fecha'}
            </div>
          </>
        )}
      </td>
    </tr>
  );
}

function Candidates({ need }: { need: Need }) {
  const candidates = need.candidates ?? [];
  return (
    <section className="panel">
      <h3>Candidatos para {needName(need)}</h3>
      <p className="lead">
        {need.role ?? 'Perfil no informado'}
        {need.project || need.tribe ? ` para ${need.project ?? need.tribe}` : ''}
        {need.neededFrom ? `, requerido el ${formatDate(need.neededFrom)}` : ''}. Ordenados por coincidencia de rol y fecha de
        disponibilidad.
      </p>
      {need.candidatesAvailable === false ? (
        <div className="state state-error compact">
          No se pudo consultar la disponibilidad en el libro, por eso no hay candidatos. No significa que no existan.
        </div>
      ) : candidates.length === 0 ? (
        <div className="state state-empty compact">No hay candidatos disponibles para esta necesidad.</div>
      ) : (
        <div className="candidate-grid">
          {candidates.map((c, i) => (
            <article key={`${c.personId}-${i}`} className="candidate">
              <div className="candidate-head">
                {c.personId ? (
                  <Link to={`/people?id=${encodeURIComponent(c.personId)}`}>{c.personName ?? c.personId}</Link>
                ) : (
                  <Missing>Persona no informada</Missing>
                )}
                <Chip tone={c.availabilityStatus === 'CONFIRMED' ? 'ok' : 'warn'}>
                  Propuesta · disponibilidad {(availabilityLabel(c.availabilityStatus) ?? 'sin estado').toLowerCase()}
                </Chip>
              </div>
              <div className="muted">
                {c.personId} · {c.personRole ?? 'perfil sin dato'} · libre desde {formatDate(c.availableFrom) ?? 'fecha no informada'}
              </div>
              <div>
                <strong>Por qué aparece:</strong> {c.reason ?? <Missing>el servicio no informó la razón.</Missing>}
              </div>
              <div>
                <strong>Coincidencia:</strong> {c.score ?? '—'} de 100
                {c.availabilityStatus !== 'CONFIRMED' ? (
                  <>
                    {' '}
                    · <strong>Falta validar:</strong> que su responsable confirme la disponibilidad.
                  </>
                ) : null}
              </div>
              <div className="muted small">
                {sourceLabel(c.source) ?? 'Fuente no informada'} · {formatDate(c.observedAt) ?? 'sin fecha de consulta'}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function NeedForm({ onCreated }: { onCreated: (need: Need) => void }) {
  const [tribe, setTribe] = useState('');
  const [project, setProject] = useState('');
  const [role, setRole] = useState('');
  const [seniority, setSeniority] = useState('');
  const [mechanism, setMechanism] = useState<Mechanism | ''>('');
  const [neededFrom, setNeededFrom] = useState('');
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const missingRole = !role.trim();
  const missingPlace = !tribe.trim() && !project.trim();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setTouched(true);
    if (missingRole || missingPlace) return;
    setSaving(true);
    setError(null);
    // Solo se envía lo que la persona escribió: la web no completa campos por su cuenta.
    const input: InformalNeedInput = { role: role.trim() };
    if (tribe.trim()) input.tribe = tribe.trim();
    if (project.trim()) input.project = project.trim();
    if (seniority.trim()) input.seniority = seniority.trim();
    if (mechanism) input.mechanism = mechanism;
    if (neededFrom) input.neededFrom = neededFrom;
    if (note.trim()) input.note = note.trim();
    try {
      onCreated(await demandApi.createNeed(input));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="need-form" onSubmit={onSubmit} noValidate>
      <label>
        Perfil requerido *
        <input value={role} onChange={(e) => setRole(e.target.value)} placeholder="p. ej. Backend Developer" />
        {touched && missingRole ? <span className="text-danger small">Indica el perfil.</span> : null}
      </label>
      <label>
        Tribu
        <input value={tribe} onChange={(e) => setTribe(e.target.value)} />
      </label>
      <label>
        Proyecto
        <input value={project} onChange={(e) => setProject(e.target.value)} />
        {touched && missingPlace ? <span className="text-danger small">Indica la tribu o el proyecto.</span> : null}
      </label>
      <label>
        Seniority
        <input value={seniority} onChange={(e) => setSeniority(e.target.value)} />
      </label>
      <label>
        Mecanismo
        <select value={mechanism} onChange={(e) => setMechanism(e.target.value as Mechanism | '')}>
          <option value="">No indicar</option>
          {MECHANISMS.map((m) => (
            <option key={m} value={m}>
              {MECHANISM_LABELS[m]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Se necesita desde
        <input type="date" value={neededFrom} onChange={(e) => setNeededFrom(e.target.value)} />
      </label>
      <label className="span-all">
        Nota
        <textarea rows={2} value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} />
      </label>
      <div className="span-all row-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Registrando…' : 'Registrar necesidad'}
        </button>
        <span className="muted small">* Obligatorio. Además se requiere la tribu o el proyecto.</span>
        {error ? <span className="text-danger">{error}</span> : null}
      </div>
    </form>
  );
}
