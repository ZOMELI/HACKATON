import { httpCasesApi, type CasesApi } from './cases';
import { httpDemandApi, type DemandApi } from './demand';
import { httpIngestionApi, type IngestionApi } from './ingestion';
import { httpLedgerApi, type LedgerApi } from './ledger';

/** La web siempre habla con los servicios reales. */
export const casesApi: CasesApi = httpCasesApi;
export const demandApi: DemandApi = httpDemandApi;
export const ledgerApi: LedgerApi = httpLedgerApi;
export const ingestionApi: IngestionApi = httpIngestionApi;
