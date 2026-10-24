import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { searchAlerts } from '../../services/alertService';
import { searchEvents } from '../../services/eventService';
import { AlertResponse } from '../../types/alerts';
import { SecurityEventResponse } from '../../types/events';
import { getTotalElements } from '../../types/pagination';
import { ServiceHealthState } from '../../types/health';
import { SeverityBadge, StatusPill } from '../common/Badge';
import { CardSkeleton, TableSkeleton } from '../common/LoadingSkeleton';
import { ErrorBanner } from '../common/ErrorBanner';
import { AlertDetailModal } from '../alerts/AlertDetailModal';
import { EventDetailModal } from '../events/EventDetailModal';
import { 
  ShieldAlert, 
  Activity, 
  Bell, 
  Server, 
  Flame, 
  RefreshCw, 
  ArrowUpRight, 
  ArrowRight,
  Cpu
} from 'lucide-react';

interface OverviewViewProps {
  health: ServiceHealthState;
  onNavigateTab: (tab: 'overview' | 'events' | 'alerts' | 'lab') => void;
}

export const OverviewView: React.FC<OverviewViewProps> = ({ health, onNavigateTab }) => {
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Metrics
  const [totalEventsCount, setTotalEventsCount] = useState<number>(0);
  const [openAlertsCount, setOpenAlertsCount] = useState<number>(0);
  const [highAlertsCount, setHighAlertsCount] = useState<number>(0);

  // Recent feeds
  const [recentAlerts, setRecentAlerts] = useState<AlertResponse[]>([]);
  const [recentEvents, setRecentEvents] = useState<SecurityEventResponse[]>([]);

  // Modals
  const [inspectAlert, setInspectAlert] = useState<AlertResponse | null>(null);
  const [inspectEvent, setInspectEvent] = useState<SecurityEventResponse | null>(null);

  const loadDashboardData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      // 1. Fetch total events
      const eventsRes = await searchEvents({ size: 5 });
      setTotalEventsCount(getTotalElements(eventsRes));
      setRecentEvents(eventsRes.content || []);

      // 2. Fetch open alerts
      const openAlertsRes = await searchAlerts({ status: 'OPEN', size: 5 });
      setOpenAlertsCount(getTotalElements(openAlertsRes));

      // 3. Fetch high / critical alerts
      const highAlertsRes = await searchAlerts({ severity: 'HIGH', size: 1 });
      setHighAlertsCount(getTotalElements(highAlertsRes));

      // 4. Fetch recent all alerts for feed
      const allAlertsRes = await searchAlerts({ size: 5 });
      setRecentAlerts(allAlertsRes.content || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to query live dashboard telemetry';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [user?.tenantSlug]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Top Welcome & Lab Architecture Banner */}
      <div className="relative overflow-hidden rounded-2xl border border-blue-500/20 bg-gradient-to-r from-blue-950/40 via-slate-900/60 to-slate-900/40 p-6">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2 text-blue-400 text-xs font-mono mb-1">
              <Cpu className="w-3.5 h-3.5" />
              <span>SOC OVERVIEW • LOCAL-FIRST ZERO-COST ARCHITECTURE</span>
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Security Operations Command Center
            </h1>
            <p className="text-xs text-slate-400 mt-1 max-w-2xl leading-relaxed">
              Real-time ingestion telemetry, deterministic detection rules, and incident triage for tenant{' '}
              <span className="text-blue-400 font-mono font-semibold">{user?.tenantSlug}</span>.
            </p>
          </div>

          <div className="flex items-center space-x-2.5">
            <button
              onClick={() => onNavigateTab('lab')}
              className="px-3.5 py-2 rounded-xl bg-rose-600/20 hover:bg-rose-600/30 text-rose-300 border border-rose-500/30 text-xs font-medium transition flex items-center space-x-1.5 shadow-sm"
            >
              <Flame className="w-3.5 h-3.5 text-rose-400" />
              <span>Launch Simulator</span>
            </button>

            <button
              onClick={loadDashboardData}
              disabled={isLoading}
              className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-medium transition flex items-center space-x-1.5 shadow-sm disabled:opacity-40"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Refresh Metrics</span>
            </button>
          </div>
        </div>
      </div>

      {error && <ErrorBanner message={error} onRetry={loadDashboardData} />}

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {isLoading ? (
          <>
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
            <CardSkeleton />
          </>
        ) : (
          <>
            {/* KPI 1: Ingested Events */}
            <div 
              onClick={() => onNavigateTab('events')}
              className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 hover:border-slate-700 transition cursor-pointer group shadow-sm"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase tracking-wider text-slate-400">Total Ingested Events</span>
                <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20">
                  <Activity className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-bold font-mono text-white tracking-tight">
                  {totalEventsCount}
                </span>
                <span className="text-[11px] font-mono text-blue-400 flex items-center space-x-0.5 group-hover:underline">
                  <span>Explore stream</span>
                  <ArrowUpRight className="w-3 h-3" />
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1 font-mono">Tenant scoped: {user?.tenantSlug}</p>
            </div>

            {/* KPI 2: Open Alerts */}
            <div 
              onClick={() => onNavigateTab('alerts')}
              className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 hover:border-slate-700 transition cursor-pointer group shadow-sm"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase tracking-wider text-slate-400">Active Open Alerts</span>
                <div className="p-2 rounded-lg bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  <Bell className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-bold font-mono text-amber-300 tracking-tight">
                  {openAlertsCount}
                </span>
                <span className="text-[11px] font-mono text-amber-400 flex items-center space-x-0.5 group-hover:underline">
                  <span>Triage queue</span>
                  <ArrowUpRight className="w-3 h-3" />
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1 font-mono">Status: OPEN</p>
            </div>

            {/* KPI 3: High/Critical Alerts */}
            <div 
              onClick={() => onNavigateTab('alerts')}
              className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 hover:border-slate-700 transition cursor-pointer group shadow-sm"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase tracking-wider text-slate-400">High / Critical Alerts</span>
                <div className="p-2 rounded-lg bg-rose-500/10 text-rose-400 border border-rose-500/20">
                  <ShieldAlert className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-bold font-mono text-rose-400 tracking-tight">
                  {highAlertsCount}
                </span>
                <span className="text-[11px] font-mono text-rose-400 flex items-center space-x-0.5 group-hover:underline">
                  <span>Priority alerts</span>
                  <ArrowUpRight className="w-3 h-3" />
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1 font-mono">Severity: HIGH | CRITICAL</p>
            </div>

            {/* KPI 4: Core Health */}
            <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase tracking-wider text-slate-400">SIEM Core Health</span>
                <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <Server className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-bold font-mono text-emerald-400 tracking-tight">
                  {health.data ? 'ONLINE' : 'PENDING'}
                </span>
                <span className="text-[11px] font-mono text-slate-400">
                  {health.latencyMs !== null ? `${health.latencyMs}ms` : '—'}
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1 font-mono">Java 21 • Spring Boot 3.4</p>
            </div>
          </>
        )}
      </div>

      {/* Two Column Layout: Recent Alerts & Ingested Event Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left: Recent Alerts */}
        <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-4 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center space-x-2">
                <Bell className="w-4 h-4 text-amber-400" />
                <h3 className="text-sm font-semibold text-white">Recent Detection Alerts</h3>
              </div>
              <button
                onClick={() => onNavigateTab('alerts')}
                className="text-xs font-mono text-blue-400 hover:text-blue-300 flex items-center space-x-1"
              >
                <span>View all</span>
                <ArrowRight className="w-3 h-3" />
              </button>
            </div>

            <div className="divide-y divide-slate-800/60 mt-2">
              {isLoading ? (
                <TableSkeleton rows={4} columns={3} />
              ) : recentAlerts.length === 0 ? (
                <div className="py-8 text-center text-slate-500 text-xs">
                  No alerts triggered yet for tenant '{user?.tenantSlug}'.
                </div>
              ) : (
                recentAlerts.map((alt) => (
                  <div
                    key={alt.id}
                    onClick={() => setInspectAlert(alt)}
                    className="py-3 flex items-center justify-between hover:bg-slate-900/40 px-2 rounded-lg transition cursor-pointer group"
                  >
                    <div className="flex items-center space-x-3">
                      <SeverityBadge severity={alt.severity} size="sm" />
                      <div>
                        <div className="text-xs font-semibold text-slate-200 group-hover:text-blue-400 transition">
                          {alt.title}
                        </div>
                        <div className="text-[11px] font-mono text-slate-500">
                          {alt.ruleKey} • {alt.eventCount} events
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      <StatusPill status={alt.status} />
                      <ArrowRight className="w-3.5 h-3.5 text-slate-600 group-hover:text-slate-300 transition" />
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-[11px] font-mono text-slate-500">
            <span>Deduplication & suppression active</span>
            <button
              onClick={() => onNavigateTab('lab')}
              className="text-rose-400 hover:underline flex items-center space-x-1"
            >
              <span>+ Generate test attack</span>
            </button>
          </div>
        </div>

        {/* Right: Recent Ingested Events */}
        <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-4 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center space-x-2">
                <Activity className="w-4 h-4 text-blue-400" />
                <h3 className="text-sm font-semibold text-white">Live Ingestion Telemetry</h3>
              </div>
              <button
                onClick={() => onNavigateTab('events')}
                className="text-xs font-mono text-blue-400 hover:text-blue-300 flex items-center space-x-1"
              >
                <span>View all</span>
                <ArrowRight className="w-3 h-3" />
              </button>
            </div>

            <div className="divide-y divide-slate-800/60 mt-2">
              {isLoading ? (
                <TableSkeleton rows={4} columns={3} />
              ) : recentEvents.length === 0 ? (
                <div className="py-8 text-center text-slate-500 text-xs">
                  No events ingested yet for tenant '{user?.tenantSlug}'.
                </div>
              ) : (
                recentEvents.map((evt) => (
                  <div
                    key={evt.id}
                    onClick={() => setInspectEvent(evt)}
                    className="py-3 flex items-center justify-between hover:bg-slate-900/40 px-2 rounded-lg transition cursor-pointer group"
                  >
                    <div className="flex items-center space-x-3">
                      <SeverityBadge severity={evt.severity} size="sm" />
                      <div>
                        <div className="text-xs font-mono font-medium text-slate-200 group-hover:text-blue-400 transition">
                          {evt.eventType}
                        </div>
                        <div className="text-[11px] font-mono text-slate-500">
                          {evt.host || evt.sourceName} {evt.sourceIp ? `• ${evt.sourceIp}` : ''}
                        </div>
                      </div>
                    </div>
                    <span className="text-[11px] font-mono text-slate-500">
                      {new Date(evt.eventTime).toLocaleTimeString([], { hour12: false })}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-[11px] font-mono text-slate-500">
            <span>Normalized schema v1.0</span>
            <span>REST API /events</span>
          </div>
        </div>
      </div>

      {/* Inspect Modals */}
      <AlertDetailModal
        alert={inspectAlert}
        onClose={() => setInspectAlert(null)}
        onAlertUpdated={(updated) => {
          setInspectAlert(updated);
          loadDashboardData();
        }}
      />

      <EventDetailModal
        event={inspectEvent}
        onClose={() => setInspectEvent(null)}
      />
    </div>
  );
};
