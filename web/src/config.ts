export type ServiceName = 'cases' | 'demand' | 'ledger' | 'ingestion';

const env = import.meta.env;

function url(value: string | undefined, fallback: string): string {
  return (value && value.trim() ? value.trim() : fallback).replace(/\/+$/, '');
}

export const BASE_URLS: Record<ServiceName, string> = {
  cases: url(env.VITE_CASES_URL, 'http://localhost:8084'),
  demand: url(env.VITE_DEMAND_URL, 'http://localhost:8085'),
  ledger: url(env.VITE_LEDGER_URL, 'http://localhost:8083'),
  ingestion: url(env.VITE_INGESTION_URL, 'http://localhost:8082'),
};

export const SERVICE_LABELS: Record<ServiceName, string> = {
  cases: 'casos',
  demand: 'demanda',
  ledger: 'libro de afirmaciones',
  ingestion: 'sincronización',
};
