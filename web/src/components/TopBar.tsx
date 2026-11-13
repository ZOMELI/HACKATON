import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { ingestionApi } from '../api';
import type { SyncResult } from '../api/ingestion';
import { errorMessage, formatDateTime, plural } from '../lib/format';
import { sourceLabel } from '../lib/labels';
import { useSession } from '../session';
import { ROLE_LABELS, SIM_USERS } from '../users';
import { Chip, Missing } from './ui';

const NAV = [
  { to: '/', label: 'Bandeja', end: true },
  { to: '/people', label: 'Ficha' },
  { to: '/availability', label: 'Disponibilidad' },
  { to: '/demand', label: 'Demanda' },
  { to: '/audit', label: 'Auditoría' },
];

export function TopBar() {
  const { user, changeUser, refresh } = useSession();
  const [syncing, setSyncing] = useState(false);
  const [result, setResult] = useState<SyncResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const inCases = useLocation().pathname.startsWith('/cases/');

  // Última sincronización conocida, para mostrar cuándo se leyeron las fuentes.
  useEffect(() => {
    let cancelled = false;
    ingestionApi.lastSync().then(
      (r) => {
        if (!cancelled) setResult(r);
      },
      (e: unknown) => {
        if (!cancelled) setError(errorMessage(e));
      },
    );
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!open) return;
    const onDown = (ev: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(ev.target as Node)) setOpen(false);
    };
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  async function runSync() {
    setSyncing(true);
    setError(null);
    setOpen(true);
    try {
      const r = await ingestionApi.sync();
      setResult(r);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSyncing(false);
      refresh(); // al terminar, la pantalla actual recarga sus datos
    }
  }

  const sources = result?.sources ?? [];
  const failed = sources.filter((s) => s.status === 'FAILED').length;
  const finished = formatDateTime(result?.finishedAt);

  const okSources = sources.filter((s) => s.status === 'OK').length;

  return (
    <div className="topbar-wrap" ref={panelRef}>
      <header className="topbar">
        <div className="brand">People &amp; Capacity</div>
        <nav className="nav" aria-label="Principal">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive || (item.to === '/' && inCases) ? 'active' : undefined)}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="topbar-right">
          <button type="button" className="btn btn-sync" onClick={runSync} disabled={syncing}>
            {syncing ? 'Sincronizando…' : 'Sincronizar fuentes'}
          </button>
          <label className="user-select">
            <span>Usuario</span>
            <select
              value={user.id}
              onChange={(e) => {
                const next = SIM_USERS.find((u) => u.id === e.target.value);
                if (next) changeUser(next);
              }}
            >
              {SIM_USERS.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name} · {ROLE_LABELS[u.role]}
                </option>
              ))}
            </select>
          </label>
        </div>
      </header>
      <div className="subbar">
        <span>
          {error
            ? 'No se pudo consultar la última sincronización'
            : finished
              ? `Última sincronización: ${finished}`
              : 'Todavía no se han sincronizado las fuentes'}
          {sources.length > 0 ? ` · ${plural(okSources, 'fuente leída', 'fuentes leídas')}` : ''}
          {failed > 0 ? ` · ${failed} con falla` : ''}
        </span>
        <button type="button" className="link-btn" onClick={() => setOpen((o) => !o)} aria-expanded={open}>
          {open ? 'Ocultar detalle' : 'Ver detalle'}
        </button>
      </div>
      {open ? (
        <div className="sync-panel" role="dialog" aria-label="Estado de la sincronización">
          <div className="sync-panel-head">
            <strong>Sincronización de fuentes</strong>
          </div>
          {syncing ? <p className="muted">Leyendo las fuentes…</p> : null}
          {error ? <p className="text-danger">{error}</p> : null}
          {!syncing && !error && sources.length === 0 ? (
            <p className="muted">Todavía no se ha ejecutado ninguna sincronización.</p>
          ) : null}
          {sources.length > 0 ? (
            <>
              <p className="muted small">
                {result?.syncId ? <span className="mono">{result.syncId}</span> : 'Sin identificador'} ·{' '}
                {finished ? `terminó el ${finished}` : 'sin fecha de término'}
              </p>
              <table className="table table-compact">
                <thead>
                  <tr>
                    <th>Fuente</th>
                    <th>Estado</th>
                    <th className="num">Registros</th>
                    <th>Leída</th>
                  </tr>
                </thead>
                <tbody>
                  {sources.map((s, i) => (
                    <tr key={`${s.source}-${i}`}>
                      <td>
                        {sourceLabel(s.source) ?? <Missing>Fuente no informada</Missing>}
                        {s.message ? <div className="muted small">{s.message}</div> : null}
                      </td>
                      <td>
                        {s.status === 'OK' ? (
                          <Chip tone="ok">Correcta</Chip>
                        ) : s.status === 'FAILED' ? (
                          <Chip tone="danger">Falló</Chip>
                        ) : (
                          <Missing>Sin estado</Missing>
                        )}
                      </td>
                      <td className="num">{s.records ?? <Missing>—</Missing>}</td>
                      <td>{formatDateTime(s.readAt) ?? <Missing>Sin fecha</Missing>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
