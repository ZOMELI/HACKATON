import type { ReactNode } from 'react';
import { formatDateTime } from '../lib/format';
import { sourceLabel } from '../lib/labels';

/** Fuente y fecha de consulta: acompaña a todo dato mostrado. Si falta, lo dice. */
export function SourceTag({ source, sourceRef, observedAt }: { source?: string | null; sourceRef?: string; observedAt?: string | null }) {
  const label = sourceLabel(source);
  const when = formatDateTime(observedAt);
  return (
    <span className="source-tag">
      <span className={label ? undefined : 'missing'}>{label ? `Fuente: ${label}` : 'Fuente no informada'}</span>
      {sourceRef ? <span className="mono"> · {sourceRef}</span> : null}
      <span className={when ? undefined : 'missing'}>{when ? ` · consultado el ${when}` : ' · fecha de consulta no informada'}</span>
    </span>
  );
}

/** Marca breve para los datos que extrajo o redactó un modelo de IA. */
export function AiNote({ children = 'Extraída por IA' }: { children?: ReactNode }) {
  return (
    <span className="ai-note" title="Este dato lo obtuvo un modelo de IA a partir del texto original.">
      {children}
    </span>
  );
}

export function Missing({ children = 'Sin dato' }: { children?: ReactNode }) {
  return <span className="missing">{children}</span>;
}

export function Chip({ tone = 'neutral', children }: { tone?: 'neutral' | 'ok' | 'solid' | 'warn' | 'danger' | 'info'; children: ReactNode }) {
  return <span className={`chip chip-${tone}`}>{children}</span>;
}

export function Loading({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="state state-loading" role="status">
      <span className="spinner" aria-hidden="true" />
      {label}
    </div>
  );
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="state state-error" role="alert">
      <div>
        <strong>No se pudieron cargar los datos.</strong>
        <div>{message}</div>
      </div>
      {onRetry ? (
        <button type="button" className="btn" onClick={onRetry}>
          Reintentar
        </button>
      ) : null}
    </div>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="state state-empty">
      <strong>{title}</strong>
      {children ? <div>{children}</div> : null}
    </div>
  );
}

export function PageHeader({ title, subtitle, children }: { title: string; subtitle?: string; children?: ReactNode }) {
  return (
    <header className="page-header">
      <div>
        <h1>
          {title}
        </h1>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      {children ? <div className="page-actions">{children}</div> : null}
    </header>
  );
}
