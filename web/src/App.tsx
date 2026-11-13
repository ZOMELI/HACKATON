import { Link, Route, Routes } from 'react-router-dom';
import { TopBar } from './components/TopBar';
import { Empty } from './components/ui';
import { AuditPage } from './pages/AuditPage';
import { AvailabilityPage } from './pages/AvailabilityPage';
import { CasesPage } from './pages/CasesPage';
import { DemandPage } from './pages/DemandPage';
import { PeoplePage } from './pages/PeoplePage';

export function App() {
  return (
    <div className="app">
      <TopBar />
      <main className="main">
        <Routes>
          <Route path="/" element={<CasesPage />} />
          <Route path="/cases/:caseId" element={<CasesPage />} />
          <Route path="/people" element={<PeoplePage />} />
          <Route path="/availability" element={<AvailabilityPage />} />
          <Route path="/demand" element={<DemandPage />} />
          <Route path="/audit" element={<AuditPage />} />
          <Route
            path="*"
            element={
              <Empty title="Esta página no existe.">
                <Link to="/">Volver a la bandeja de casos</Link>
              </Empty>
            }
          />
        </Routes>
      </main>
    </div>
  );
}
