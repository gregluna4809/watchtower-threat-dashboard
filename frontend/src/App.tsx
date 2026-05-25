import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { ConnectionDetailPage } from './pages/ConnectionDetailPage';
import { ConnectionsPage } from './pages/ConnectionsPage';
import { DashboardPage } from './pages/DashboardPage';
import { EndpointDetailPage } from './pages/EndpointDetailPage';
import { EndpointsPage } from './pages/EndpointsPage';
import { ProcessDetailPage } from './pages/ProcessDetailPage';
import { ProcessesPage } from './pages/ProcessesPage';
import { RulesPage } from './pages/RulesPage';

function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/connections" element={<ConnectionsPage />} />
        <Route path="/connections/:id" element={<ConnectionDetailPage />} />
        <Route path="/processes" element={<ProcessesPage />} />
        <Route path="/processes/:id" element={<ProcessDetailPage />} />
        <Route path="/endpoints" element={<EndpointsPage />} />
        <Route path="/endpoints/:id" element={<EndpointDetailPage />} />
        <Route path="/rules" element={<RulesPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}

export default App;
