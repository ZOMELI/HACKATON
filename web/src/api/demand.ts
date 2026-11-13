// Tipos derivados a mano de contracts/demand.yaml
import { request } from './http';

export type NeedStatus = 'OPEN' | 'COVERED' | 'CANCELLED';

export const MECHANISMS = ['INTERNAL_MOVE', 'EXTERNAL_HIRE', 'PROVIDER', 'TEMP_SUPPORT', 'UNDEFINED'] as const;
export type Mechanism = (typeof MECHANISMS)[number];

export type RecruitmentState = 'NOT_STARTED' | 'SOURCING' | 'INTERVIEWS' | 'OFFER' | 'HIRED' | 'CANCELLED';

export interface RecruitmentStatus {
  status?: RecruitmentState;
  source?: string;
  observedAt?: string;
}

export interface Candidate {
  personId?: string;
  personName?: string;
  personRole?: string;
  availableFrom?: string;
  availabilityStatus?: 'PLANNED' | 'CONFIRMED';
  source?: string;
  observedAt?: string;
  score?: number;
  reason?: string;
}

export interface Need {
  id: string;
  externalId?: string | null;
  informal?: boolean;
  tribe?: string;
  project?: string;
  role?: string;
  seniority?: string;
  mechanism?: Mechanism;
  neededFrom?: string;
  status?: NeedStatus;
  source?: string;
  observedAt?: string;
  recruitment?: RecruitmentStatus | null;
  note?: string;
  createdBy?: string;
  candidates?: Candidate[];
  candidatesAvailable?: boolean;
}

export interface InformalNeedInput {
  tribe?: string;
  project?: string;
  role: string;
  seniority?: string;
  mechanism?: Mechanism;
  neededFrom?: string;
  note?: string;
}

export interface DemandApi {
  listNeeds(status?: NeedStatus): Promise<Need[]>;
  createNeed(input: InformalNeedInput): Promise<Need>;
}

export const httpDemandApi: DemandApi = {
  listNeeds: (status) => request<Need[]>('demand', '/needs', { query: { status } }),
  createNeed: (input) =>
    request<Need>('demand', '/needs', {
      method: 'POST',
      body: input,
      statusMessages: { 400: 'Falta la tribu o el proyecto, o falta el perfil.' },
    }),
};
