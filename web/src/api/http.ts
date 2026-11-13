import { BASE_URLS, SERVICE_LABELS, type ServiceName } from '../config';
import { loadStoredUser, type SimUser } from '../users';

/** Usuario simulado vigente. Se envía en TODAS las llamadas como X-User-Id / X-User-Role. */
let sessionUser: SimUser = loadStoredUser();

export function getSessionUser(): SimUser {
  return sessionUser;
}

export function setSessionUser(user: SimUser): void {
  sessionUser = user;
}

export class ApiError extends Error {
  readonly status?: number;
  readonly service: ServiceName;

  constructor(service: ServiceName, message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.service = service;
    this.status = status;
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT';
  query?: Record<string, string | undefined>;
  body?: unknown;
  /** Mensajes en español por código HTTP, tomados del contrato. */
  statusMessages?: Record<number, string>;
}

async function readServerMessage(res: Response): Promise<string | undefined> {
  try {
    const text = await res.text();
    if (!text) return undefined;
    try {
      const json: unknown = JSON.parse(text);
      if (json && typeof json === 'object') {
        const obj = json as Record<string, unknown>;
        for (const key of ['message', 'detail', 'error']) {
          if (typeof obj[key] === 'string') return obj[key] as string;
        }
      }
      return undefined;
    } catch {
      return text.length <= 200 ? text : undefined;
    }
  } catch {
    return undefined;
  }
}

export async function request<T>(service: ServiceName, path: string, options: RequestOptions = {}): Promise<T> {
  const base = BASE_URLS[service];
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(options.query ?? {})) {
    if (value !== undefined && value !== '') params.set(key, value);
  }
  const qs = params.toString();
  const target = `${base}${path}${qs ? `?${qs}` : ''}`;

  const headers: Record<string, string> = {
    Accept: 'application/json',
    'X-User-Id': sessionUser.id,
    'X-User-Role': sessionUser.role,
  };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';

  let res: Response;
  try {
    res = await fetch(target, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(
      service,
      `No se pudo conectar con el servicio de ${SERVICE_LABELS[service]} (${base}). Verifica que esté levantado y que permita CORS.`,
    );
  }

  if (!res.ok) {
    const known = options.statusMessages?.[res.status];
    const fromServer = await readServerMessage(res);
    const detail = known ?? fromServer ?? `El servicio de ${SERVICE_LABELS[service]} respondió con un error.`;
    throw new ApiError(service, `${detail} (HTTP ${res.status})`, res.status);
  }

  if (res.status === 204) return undefined as T;
  try {
    return (await res.json()) as T;
  } catch {
    throw new ApiError(service, `El servicio de ${SERVICE_LABELS[service]} devolvió una respuesta que no es JSON válido.`, res.status);
  }
}
