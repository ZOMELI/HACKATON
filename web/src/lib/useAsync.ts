import { useCallback, useEffect, useRef, useState } from 'react';
import { useSession } from '../session';
import { errorMessage } from './format';

interface State<T> {
  data?: T;
  error?: string;
  loading: boolean;
  scope: string;
}

export interface AsyncResult<T> {
  data: T | undefined;
  error: string | undefined;
  loading: boolean;
  reload: () => void;
  setData: (data: T) => void;
}

/**
 * Carga datos y los recarga cuando cambia el usuario simulado, tras sincronizar fuentes,
 * o cuando cambian `deps`. Nunca muestra datos cargados con otro usuario u otros parámetros.
 */
export function useAsync<T>(fn: () => Promise<T>, deps: readonly unknown[], enabled = true): AsyncResult<T> {
  const { user, refreshKey } = useSession();
  const scope = `${user.id}|${JSON.stringify(deps)}`;
  const [state, setState] = useState<State<T>>({ loading: enabled, scope });
  const [tick, setTick] = useState(0);
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    if (!enabled) {
      setState({ loading: false, scope });
      return;
    }
    let cancelled = false;
    setState((s) => ({ ...s, loading: true, error: undefined }));
    fnRef.current().then(
      (data) => {
        if (!cancelled) setState({ data, loading: false, scope });
      },
      (error: unknown) => {
        if (!cancelled) setState({ error: errorMessage(error), loading: false, scope });
      },
    );
    return () => {
      cancelled = true;
    };
  }, [scope, refreshKey, tick, enabled]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  const setData = useCallback((data: T) => setState({ data, loading: false, scope }), [scope]);

  const fresh = state.scope === scope;
  return {
    data: fresh ? state.data : undefined,
    error: fresh ? state.error : undefined,
    loading: enabled && (state.loading || !fresh),
    reload,
    setData,
  };
}
