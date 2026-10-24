import React, { useState, useEffect, useCallback } from 'react';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { OverviewView } from './components/overview/OverviewView';
import { EventsView } from './components/events/EventsView';
import { AlertsView } from './components/alerts/AlertsView';
import { IncidentsView } from './components/incidents/IncidentsView';
import { LabSimulatorView } from './components/lab/LabSimulatorView';
import { ThreatIntelView } from './components/threatintel/ThreatIntelView';
import { fetchHealth } from './services/api';
import { ServiceHealthState } from './types/health';

const MainAppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'events' | 'alerts' | 'incidents' | 'threat-intel' | 'lab'>('overview');
  const [health, setHealth] = useState<ServiceHealthState>({
    isChecking: true,
    data: null,
    error: null,
    latencyMs: null,
    lastChecked: null,
  });

  const checkBackendHealth = useCallback(async () => {
    setHealth((prev) => ({ ...prev, isChecking: true }));
    try {
      const { data, latencyMs } = await fetchHealth();
      setHealth({
        isChecking: false,
        data,
        error: null,
        latencyMs,
        lastChecked: new Date().toLocaleTimeString(),
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unknown network failure';
      setHealth({
        isChecking: false,
        data: null,
        error: message,
        latencyMs: null,
        lastChecked: new Date().toLocaleTimeString(),
      });
    }
  }, []);

  useEffect(() => {
    checkBackendHealth();
    const interval = setInterval(checkBackendHealth, 20000);
    return () => clearInterval(interval);
  }, [checkBackendHealth]);

  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100 flex flex-col font-sans selection:bg-blue-600 selection:text-white">
      {/* Top Navbar */}
      <Navbar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        health={health}
        onRefreshHealth={checkBackendHealth}
      />

      {/* Main Tabbed Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6">
        {activeTab === 'overview' && (
          <OverviewView
            health={health}
            onNavigateTab={(tab) => setActiveTab(tab as 'overview' | 'events' | 'alerts' | 'incidents' | 'threat-intel' | 'lab')}
          />
        )}

        {activeTab === 'events' && <EventsView />}

        {activeTab === 'alerts' && (
          <AlertsView
            onNavigateToIncidents={() => setActiveTab('incidents')}
          />
        )}

        {activeTab === 'incidents' && <IncidentsView />}

        {activeTab === 'threat-intel' && <ThreatIntelView />}

        {activeTab === 'lab' && (
          <LabSimulatorView
            onNavigateToAlerts={() => setActiveTab('alerts')}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-4 px-6 text-center text-xs text-slate-500 font-mono">
        SentinelX Security Operations Center • Local-First Architecture • ₹0 Development Tier • Phase 6 Incident Management Console
      </footer>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <MainAppContent />
    </AuthProvider>
  );
};

export default App;
