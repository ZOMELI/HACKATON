import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { casesApi, ledgerApi } from '../api';
import { DIMENSIONS, type Claim, type DimensionView, type Profile } from '../api/ledger';
import { Attributes, claimValueText, Validity } from '../components/ClaimBits';
import { EvidenceCard } from '../components/Evidence';
import { Chip, Empty, ErrorBox, Loading, Missing } from '../components/ui';
import { formatDate, plural } from '../lib/format';
import { caseTypeLabel, dimensionStatusLabel, sourceLabel, subjectLabel } from '../lib/labels';
import { useAsync } from '../lib/useAsync';

export function PeoplePage() {
  const [params, setParams] = useSearchParams();
  const query = params.get('q') ?? '';
  const personId = params.get('id') ?? '';
  const [draft, setDraft] = useState(query);

  const results = useAsync(() => ledgerApi.searchPeople(query), [query], !personId);
  const people = results.data ?? [];
  // Si la búsqueda devuelve una sola persona, se abre su ficha directamente.
  const shownId = personId || (query && people.length === 1 ? people[0].personId : '');

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const next = new URLSearchParams();
    if (draft.trim()) next.set('q', draft.trim());
    setParams(next);
  }

  return (
    <>
      <form onSubmit={onSubmit}>
        <label htmlFor="person-search">
          <span>
            Buscar por matrícula, nombre o correo
          </span>
        </label>
        <div className="search" style={{ marginTop: 6 }}>
          <input id="person-search" type="search" value={draft} onChange={(e) => setDraft(e.target.value)} />
          <button type="submit" className="btn btn-primary">
            Buscar
          </button>
        </div>
      </form>

      {shownId ? (
        <PersonFile key={shownId} personId={shownId} />
      ) : (
        <section className="person-results" style={{ marginTop: 22 }}>
          {results.error ? <ErrorBox message={results.error} onRetry={results.reload} /> : null}
          {results.loading && !results.data ? <Loading label="Buscando…" /> : null}
          {results.data && people.length === 0 ? (
            <Empty title={query ? `Sin resultados para «${query}».` : 'El libro no devolvió personas.'}>
              Prueba con la matrícula completa, parte del nombre o el correo.
            </Empty>
          ) : null}
          {people.length > 0 ? (
            <>
              <p className="muted">
                {plural(people.length, 'persona', 'personas')}
                {query ? ` para «${query}»` : ''}. Elige una para ver su ficha.
              </p>
              <ul className="case-list">
                {people.map((p) => (
                  <li key={p.personId}>
                    <Link to={`/people?id=${encodeURIComponent(p.personId)}`} className="case-row">
                      <div className="case-row-title">{p.name ?? <Missing>Sin nombre</Missing>}</div>
                      <div className="case-row-meta">
                        {p.personId} · {p.role ?? 'perfil sin dato'} · {p.email ?? 'sin correo'}
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          ) : null}
        </section>
      )}
    </>
  );
}

function PersonFile({ personId }: { personId: string }) {
  const profile = useAsync(() => ledgerApi.getProfile(personId), [personId]);
  const history = useAsync(() => ledgerApi.getHistory(personId), [personId]);
  const openCases = useAsync(() => casesApi.listCases({ personId, status: 'OPEN' }), [personId]);
  const person = profile.data?.person;

  return (
    <>
      <header className="person-head">
        <div>
          <h1>{person?.name ?? (profile.loading ? 'Cargando…' : personId)}</h1>
          <p>
            {person?.personId ?? personId} · {person?.role ?? 'perfil sin dato'} · {person?.email ?? 'sin correo'}
          </p>
        </div>
        <div className="case-badges">
          {openCases.error ? <span className="text-danger small">No se pudieron consultar sus casos.</span> : null}
          {openCases.data && openCases.data.length === 0 ? <span className="muted">Sin casos abiertos a tu cargo</span> : null}
          {(openCases.data ?? []).map((c) => (
            <Link key={c.id} to={`/cases/${encodeURIComponent(c.id)}`} className="case-badge">
              Caso abierto: {caseTypeLabel(c.type).toLowerCase()}
            </Link>
          ))}
        </div>
      </header>

      <div className="split-wide">
        <section className="panel">
          <h3>Las cuatro dimensiones</h3>
          <p className="lead">Cada dato muestra de qué fuente viene y cuándo se consultó. Las dimensiones no se mezclan.</p>
          {profile.error ? <ErrorBox message={profile.error} onRetry={profile.reload} /> : null}
          {profile.loading && !profile.data ? <Loading label="Cargando la ficha…" /> : null}
          {profile.data ? <Dimensions profile={profile.data} /> : null}
        </section>

        <section className="panel">
          <h3>Línea de tiempo</h3>
          {history.error ? <ErrorBox message={history.error} onRetry={history.reload} /> : null}
          {history.loading && !history.data ? <Loading label="Cargando el historial…" /> : null}
          {history.data && history.data.length === 0 ? (
            <p className="muted">El libro no tiene afirmaciones registradas para esta persona.</p>
          ) : null}
          {history.data && history.data.length > 0 ? (
            <ol className="timeline">
              {history.data.map((c) => (
                <TimelineItem key={c.id} claim={c} />
              ))}
            </ol>
          ) : null}
        </section>
      </div>
    </>
  );
}

function Dimensions({ profile }: { profile: Profile }) {
  const grade = profile.salaryGrade;
  return (
    <>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Dimensión</th>
              <th>Valor</th>
              <th>Fuente y fecha</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {DIMENSIONS.map((subject) => (
              <DimensionRow key={subject} subject={subject} view={profile.dimensions?.find((d) => d.subject === subject)} />
            ))}
          </tbody>
        </table>
      </div>
      <div className="grade-row">
        <strong>Grado salarial</strong>
        <span>
          {!grade
            ? 'El libro no informó este dato.'
            : grade.visible !== true
              ? 'Oculto para tu rol. Solo lo consulta un CAL o el gerente de su jerarquía.'
              : `${grade.value ?? 'Sin dato'}${sourceLabel(grade.source) ? ` · ${sourceLabel(grade.source)}` : ''}${
                  grade.observedAt ? ` · ${formatDate(grade.observedAt)}` : ''
                }`}
        </span>
      </div>
    </>
  );
}

function sourceLine(source: string | undefined, observedAt: string | undefined) {
  return (
    <>
      {sourceLabel(source) ?? <Missing>Fuente no informada</Missing>} · {formatDate(observedAt) ?? <Missing>sin fecha de consulta</Missing>}
    </>
  );
}

function DimensionRow({ subject, view }: { subject: string; view: DimensionView | undefined }) {
  const status = view?.status;
  const hasValue = !!(view?.displayValue ?? view?.value);
  const pending = status === 'PENDING_CONFIRMATION';
  const missing = !view || status === 'MISSING' || (!hasValue && !pending);
  // Las alternativas son las otras versiones; se omite la que repite exactamente la principal.
  const alternatives = (view?.alternatives ?? []).filter((a) => !(a.value === view?.value && a.source === view?.source));

  return (
    <tr>
      <td>
        <strong>{subjectLabel(subject)}</strong>
      </td>
      {missing ? (
        <td colSpan={2}>
          <Missing>{view ? 'No hay ninguna afirmación vigente para esta dimensión.' : 'El libro no informó esta dimensión.'}</Missing>
        </td>
      ) : (
        <>
          <td>
            {pending ? <div className="version-label">Versión 1</div> : null}
            {view?.displayValue ?? view?.value ?? <Missing>Sin valor</Missing>}
            {alternatives.map((alt, i) => (
              <div className="alt-version" key={alt.id}>
                <div className="version-label">Versión {i + 2}</div>
                {alt.value}
                {alt.attributes?.managerName ? ` · ${alt.attributes.managerName}` : ''}
                {alt.attributes?.squad ? ` · ${alt.attributes.squad}` : ''}
              </div>
            ))}
            {pending && alternatives.length === 0 ? (
              <div className="sub small">El libro la marcó como pendiente, pero no envió la otra versión.</div>
            ) : null}
          </td>
          <td>
            {pending ? <div className="version-label">&nbsp;</div> : null}
            {sourceLine(view?.source, view?.observedAt)}
            {alternatives.map((alt) => (
              <div className="alt-version" key={alt.id}>
                <div className="version-label">&nbsp;</div>
                {sourceLine(alt.source, alt.observedAt)}
              </div>
            ))}
          </td>
        </>
      )}
      <td>
        {!view ? (
          <Chip>Sin dato</Chip>
        ) : (
          <Chip tone={pending ? 'warn' : 'neutral'}>{dimensionStatusLabel(status)}</Chip>
        )}
      </td>
    </tr>
  );
}

function TimelineItem({ claim }: { claim: Claim }) {
  return (
    <li className="timeline-item">
      <div className="timeline-date">
        {formatDate(claim.recordedAt) ?? 'Sin fecha de registro'} · {sourceLabel(claim.source) ?? 'fuente no informada'}
      </div>
      <div className="timeline-body">
        {subjectLabel(claim.subject)}: <strong>{claimValueText(claim.subject, claim.value) ?? 'sin valor'}</strong>
        {claim.supersedes ? <span className="muted"> (reemplaza un registro anterior)</span> : null}
        <Attributes attributes={claim.attributes} />
        <Validity from={claim.validFrom} until={claim.validUntil} />
        {(claim.evidence ?? []).map((e, i) => (
          <EvidenceCard key={i} evidence={e} compact />
        ))}
      </div>
    </li>
  );
}
