import { formatDate } from '../lib/format';
import { attributeLabel, valueLabel } from '../lib/labels';

export function claimValueText(subject: string | undefined, value: string | undefined): string | null {
  if (!value) return null;
  return subject === 'AVAILABILITY' ? (valueLabel(value) ?? value) : value;
}

export function Attributes({ attributes }: { attributes?: Record<string, string> }) {
  const entries = Object.entries(attributes ?? {});
  if (entries.length === 0) return null;
  return (
    <dl className="kv">
      {entries.map(([key, value]) => (
        <div key={key} className="kv-row">
          <dt>{attributeLabel(key)}</dt>
          <dd>{valueLabel(value) ?? value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function Validity({ from, until }: { from?: string; until?: string }) {
  if (!from && !until) return null;
  return (
    <div className="validity">
      {from ? <span>Desde {formatDate(from)}</span> : null}
      {from && until ? ' · ' : null}
      {until ? <span>Hasta {formatDate(until)}</span> : null}
    </div>
  );
}
