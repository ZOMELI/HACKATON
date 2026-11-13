import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { casesApi } from '../api';
import type { AlertSettings } from '../api/cases';
import { Chip, Empty, ErrorBox, Loading, Missing, PageHeader } from '../components/ui';
import { daysUntil, errorMessage, formatDate, formatDateTime, plural } from '../lib/format';
import { availabilityLabel, sourceLabel } from '../lib/labels';
import { useAsync } from '../lib/useAsync';

function relativeDay(date: string | undefined): string | null {
  const days = daysUntil(date);
  if (days === null) return null;
  if (days === 0) return 'hoy';
  return days > 0 ? `en ${plural(days, 'día', 'días')}` : `hace ${plural(-days, 'día', 'días')}`;
}

export function AvailabilityPage() {
  const availability = useAsync(() => casesApi.listAvailability(), []);
  const settings = useAsync(() => casesApi.getAlertSettings(), []);
  const items = availability.data ?? [];

  return (
    <>
      <PageHeader
        title="Disponibilidad"
        subtitle="Un vencimiento es un aviso. La persona queda disponible cuando su responsable lo confirma."
      >
        <section className="panel settings" aria-label="Días de anticipación de las alertas">
          {settings.error ? <ErrorBox message={settings.error} onRetry={settings.reload} /> : null}
          {settings.loading && !settings.data ? <Loading label="Cargando la configuración…" /> : null}
          {settings.data ? (
            <SettingsForm
              settings={settings.data}
              onSaved={(saved) => {
                settings.setData(saved);
                availability.reload();
              }}
            />
          ) : null}
        </section>
      </PageHeader>

      <div className="stack">
        <section className="panel">
          {availability.error ? <ErrorBox message={availability.error} onRetry={availability.reload} /> : null}
          {availability.loading && !availability.data ? <Loading label="Cargando disponibilidad…" /> : null}
          {availability.data && items.length === 0 ? (
            <Empty title="No hay disponibilidad registrada visible para este usuario." />
          ) : null}
          {items.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Persona</th>
                    <th>Disponible desde</th>
                    <th>Estado</th>
                    <th>Quién confirma</th>
                    <th>Fuente y fecha</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, i) => (
                    <tr key={`${item.personId}-${i}`}>
                      <td>
                        {item.personId ? (
                          <Link to={`/people?id=${encodeURIComponent(item.personId)}`}>{item.personName ?? item.personId}</Link>
                        ) : (
                          <Missing>Persona no informada</Missing>
                        )}
                        <div className="sub">
                          {item.personId} · {item.personRole ?? 'perfil sin dato'}
                        </div>
                      </td>
                      <td>
                        {formatDate(item.availableFrom) ?? <Missing>Sin fecha</Missing>}
                        {relativeDay(item.availableFrom) ? <div className="sub">{relativeDay(item.availableFrom)}</div> : null}
                      </td>
                      <td>
                        <Chip tone={item.status === 'CONFIRMED' ? 'solid' : item.status === 'PLANNED' ? 'warn' : 'neutral'}>
                          {availabilityLabel(item.status)}
                        </Chip>
                        {item.alert ? <div className="sub">Sin confirmar y próxima</div> : null}
                      </td>
                      <td>{item.confirmerName ?? item.confirmerId ?? <Missing>No informado</Missing>}</td>
                      <td>
                        {sourceLabel(item.source) ?? <Missing>Fuente no informada</Missing>}
                        <div className="sub">{formatDate(item.observedAt) ?? 'sin fecha de consulta'}</div>
                      </td>
                      <td>
                        {item.caseId ? (
                          <Link to={`/cases/${encodeURIComponent(item.caseId)}`}>Ver caso</Link>
                        ) : (
                          <span className="muted">Sin caso</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
        <p className="footnote">
          Prevista: una fuente indica desde cuándo quedaría libre la persona, pero su responsable aún no lo confirma. Confirmada: el
          responsable indicó que la persona queda libre.
        </p>
      </div>
    </>
  );
}

function SettingsForm({ settings, onSaved }: { settings: AlertSettings; onSaved: (s: AlertSettings) => void }) {
  const [availabilityDays, setAvailabilityDays] = useState(String(settings.availabilityDays ?? ''));
  const [contractExpiryDays, setContractExpiryDays] = useState(String(settings.contractExpiryDays ?? ''));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedNow, setSavedNow] = useState(false);

  useEffect(() => {
    setAvailabilityDays(String(settings.availabilityDays ?? ''));
    setContractExpiryDays(String(settings.contractExpiryDays ?? ''));
  }, [settings]);

  const parse = (v: string) => (/^\d+$/.test(v.trim()) ? Number(v.trim()) : NaN);
  const a = parse(availabilityDays);
  const c = parse(contractExpiryDays);
  const valid = [a, c].every((n) => Number.isInteger(n) && n >= 1 && n <= 365);
  const dirty = a !== settings.availabilityDays || c !== settings.contractExpiryDays;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setSaving(true);
    setError(null);
    setSavedNow(false);
    try {
      const saved = await casesApi.putAlertSettings({ availabilityDays: a, contractExpiryDays: c });
      setSavedNow(true);
      onSaved(saved);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="settings-form" onSubmit={onSubmit}>
      <label>
        Sin confirmar (días antes)
        <input type="number" min={1} max={365} value={availabilityDays} onChange={(e) => setAvailabilityDays(e.target.value)} />
      </label>
      <label>
        Acta por vencer (días antes)
        <input type="number" min={1} max={365} value={contractExpiryDays} onChange={(e) => setContractExpiryDays(e.target.value)} />
      </label>
      <button type="submit" className="btn" disabled={!valid || !dirty || saving}>
        {saving ? 'Guardando…' : 'Guardar'}
      </button>
      {!valid || error || savedNow || settings.updatedAt ? (
        <div className="settings-note small">
          {!valid ? <span className="text-danger">Cada valor debe ser un número entero entre 1 y 365.</span> : null}
          {error ? <span className="text-danger">{error}</span> : null}
          {valid && !error ? (
            <span className="muted">
              {savedNow ? 'Guardado. ' : ''}
              {settings.updatedAt
                ? `Última modificación: ${formatDateTime(settings.updatedAt)} por ${settings.updatedBy ?? 'usuario no informado'}.`
                : ''}
            </span>
          ) : null}
        </div>
      ) : null}
    </form>
  );
}
