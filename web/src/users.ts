export type UserRole = 'CL' | 'CAL' | 'MANAGER';

export interface SimUser {
  id: string;
  name: string;
  role: UserRole;
}

// Personas que existen en SIGA (mock-sources): su jerarquía decide qué casos ve cada una.
export const SIM_USERS: SimUser[] = [
  { id: 'B10132', name: 'Tello Espinoza, Carlos', role: 'CL' },
  { id: 'B10169', name: 'Chávez Arce, María', role: 'CL' },
  { id: 'B10206', name: 'Mendoza Núñez, Daniela', role: 'CL' },
  { id: 'B10058', name: 'Mendoza Campos, Carlos', role: 'CAL' },
  { id: 'B10021', name: 'Castillo Pacheco, Milagros', role: 'MANAGER' },
];

export const ROLE_LABELS: Record<UserRole, string> = {
  CL: 'Chapter Lead',
  CAL: 'Chapter Area Lead',
  MANAGER: 'Gerente',
};

const STORAGE_KEY = 'cio.simUserId';

export function loadStoredUser(): SimUser {
  try {
    const id = window.localStorage.getItem(STORAGE_KEY);
    const found = SIM_USERS.find((u) => u.id === id);
    if (found) return found;
  } catch {
    // localStorage no disponible: se usa el usuario por defecto
  }
  return SIM_USERS[0];
}

export function storeUser(user: SimUser): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, user.id);
  } catch {
    // sin persistencia; la selección vive solo en memoria
  }
}
