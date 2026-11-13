// Tipos que usa la barra superior.
import { request } from './http';

export interface SyncSource {
  source?: string;
  status?: 'OK' | 'FAILED';
  records?: number;
  readAt?: string;
  message?: string;
}

export interface SyncResult {
  syncId?: string;
  startedAt?: string;
  finishedAt?: string;
  sources?: SyncSource[];
}

export interface IngestionApi {
  sync(): Promise<SyncResult>;
  lastSync(): Promise<SyncResult>;
}

// ingestion-service real (services/ingestion-service, openapi.yml): POST /sync y GET /sync-runs.
interface RawRun {
  id: number;
  startedAt?: string;
  finishedAt?: string;
  status?: string;
  claimsSent?: number;
  sources?: { source?: string; status?: string; records?: number; message?: string | null }[];
}

function toResult(run: RawRun | undefined): SyncResult {
  if (!run) return { sources: [] };
  return {
    syncId: `Sincronización ${run.id}`,
    startedAt: run.startedAt,
    finishedAt: run.finishedAt,
    // Las fuentes sin adaptador (NOT_IMPLEMENTED) no se leyeron: no se listan como leídas ni como fallidas.
    sources: (run.sources ?? [])
      .filter((s) => s.status === 'OK' || s.status === 'FAILED')
      .map((s) => ({
        source: s.source,
        status: s.status as 'OK' | 'FAILED',
        records: s.records,
        readAt: run.finishedAt,
        message: s.message ?? undefined,
      })),
  };
}

export const httpIngestionApi: IngestionApi = {
  sync: async () => toResult(await request<RawRun>('ingestion', '/sync', { method: 'POST' })),
  lastSync: async () => toResult((await request<RawRun[]>('ingestion', '/sync-runs'))[0]),
};
