import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { getSessionUser, setSessionUser } from './api/http';
import { storeUser, type SimUser } from './users';

interface SessionValue {
  user: SimUser;
  changeUser: (user: SimUser) => void;
  /** Cambia cada vez que hay que recargar los datos de la pantalla (p. ej. tras sincronizar). */
  refreshKey: number;
  refresh: () => void;
}

const SessionContext = createContext<SessionValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SimUser>(() => getSessionUser());
  const [refreshKey, setRefreshKey] = useState(0);

  const changeUser = useCallback((next: SimUser) => {
    setSessionUser(next); // antes del re-render: las recargas ya salen con las cabeceras nuevas
    storeUser(next);
    setUser(next);
  }, []);

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), []);

  const value = useMemo(() => ({ user, changeUser, refreshKey, refresh }), [user, changeUser, refreshKey, refresh]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error('useSession debe usarse dentro de SessionProvider');
  return ctx;
}
