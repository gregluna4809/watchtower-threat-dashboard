import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { ConnectionsPage } from './pages/ConnectionsPage';
import { DashboardPage } from './pages/DashboardPage';
import { EndpointsPage } from './pages/EndpointsPage';
import { ProcessesPage } from './pages/ProcessesPage';
import { RulesPage } from './pages/RulesPage';

function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/connections" element={<ConnectionsPage />} />
        <Route path="/processes" element={<ProcessesPage />} />
        <Route path="/endpoints" element={<EndpointsPage />} />
        <Route path="/rules" element={<RulesPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}

export default App;
