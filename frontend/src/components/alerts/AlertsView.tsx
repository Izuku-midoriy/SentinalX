import React, { useState, useEffect, useCallback } from 'react';
import { AlertResponse, AlertStatus } from '../../types/alerts';
import { PageResponse, getTotalElements, getTotalPages } from '../../types/pagination';
import { searchAlerts, updateAlert } from '../../services/alertService';
import { SeverityBadge, StatusPill } from '../common/Badge';
import { Pagination } from '../common/Pagination';
import { TableSkeleton } from '../common/LoadingSkeleton';
import { EmptyState } from '../common/EmptyState';
import { ErrorBanner } from '../common/ErrorBanner';
import { AlertDetailModal } from './AlertDetailModal';
import { useAuth } from '../../context/AuthContext';
import { RefreshCw, Layers, ShieldCheck, ChevronRight } from 'lucide-react';

export const AlertsView: React.FC<{ onNavigateToIncidents?: () => void }> = ({ onNavigateToIncidents }) => {
  const { user } = useAuth();
  const [alertsPage, setAlertsPage] = useState<PageResponse<AlertResponse> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [activeStatusTab, setActiveStatusTab] = useState<AlertStatus | 'ALL'>('ALL');
  const [selectedSeverity, setSelectedSeverity] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);

  // Modal inspection
  const [selectedAlert, setSelectedAlert] = useState<AlertResponse | null>(null);

  const loadAlerts = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await searchAlerts({
        status: activeStatusTab !== 'ALL' ? activeStatusTab : undefined,
        severity: selectedSeverity !== 'ALL' ? selectedSeverity : undefined,
        page: currentPage,
        size: pageSize,
      });
      setAlertsPage(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to query security alerts';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [activeStatusTab, selectedSeverity, currentPage, pageSize, user?.tenantSlug]);

  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  const handleAlertUpdated = (updated: AlertResponse) => {
    setSelectedAlert(updated);
    // Refresh page list
    loadAlerts();
  };

  const handleInlineStatusChange = async (e: React.MouseEvent, alertId: string, newStatus: AlertStatus) => {
    e.stopPropagation();
    try {
      await updateAlert(alertId, { status: newStatus });
      loadAlerts();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Status update failed');
    }
  };

  const totalElements = getTotalElements(alertsPage);
  const totalPages = getTotalPages(alertsPage);
  const alertsList = alertsPage?.content || [];

  const statusTabs: (AlertStatus | 'ALL')[] = ['ALL', 'OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED'];

  return (
    <div className="space-y-5 animate-fadeIn">
      {/* Top Controls: Status Tabs & Severity Dropdown */}
      <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-4 flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Status Pill Tabs */}
        <div className="flex items-center space-x-1.5 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0">
          {statusTabs.map((tab) => (
            <button
              key={tab}
              onClick={() => {
                setActiveStatusTab(tab);
                setCurrentPage(0);
              }}
              className={`px-3 py-1.5 rounded-lg text-xs font-mono font-medium transition whitespace-nowrap ${
                activeStatusTab === tab
                  ? 'bg-blue-600 text-white shadow-sm font-semibold'
                  : 'bg-slate-900/60 hover:bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-800/80'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Severity & Refresh */}
        <div className="flex items-center space-x-2 w-full sm:w-auto justify-end">
          <select
            value={selectedSeverity}
            onChange={(e) => {
              setSelectedSeverity(e.target.value);
              setCurrentPage(0);
            }}
            className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-blue-500 font-mono cursor-pointer"
          >
            <option value="ALL">Severity: All</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>

          <button
            type="button"
            onClick={loadAlerts}
            disabled={isLoading}
            title="Refresh alerts"
            className="p-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-white transition disabled:opacity-40"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Error Banner */}
      {error && <ErrorBanner message={error} onRetry={loadAlerts} />}

      {/* Main Alert Queue Card */}
      <div className="rounded-xl border border-slate-800 bg-[#0c1220] overflow-hidden shadow-sm">
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <h3 className="text-sm font-semibold text-white tracking-tight">Active Detection Alerts</h3>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-800">
              Tenant: {user?.tenantSlug || 'loading'}
            </span>
          </div>
          <span className="text-xs font-mono text-slate-400">
            {totalElements} alerts recorded
          </span>
        </div>

        {/* Table / List */}
        <div className="overflow-x-auto">
          {isLoading ? (
            <TableSkeleton rows={6} columns={6} />
          ) : alertsList.length === 0 ? (
            <EmptyState
              icon={<ShieldCheck className="w-8 h-8 text-emerald-400" />}
              title="No Alerts Found"
              description={`There are no alerts matching status '${activeStatusTab}' for tenant '${user?.tenantSlug}'. Ingest attacks via the Attack Simulator tab to see detection in action.`}
              actionText="Reset Filter to ALL"
              onAction={() => setActiveStatusTab('ALL')}
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800/80 bg-slate-900/40 text-[11px] font-mono uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Rule & Alert Title</th>
                  <th className="py-3 px-4">Linked Evidence</th>
                  <th className="py-3 px-4">Timeline</th>
                  <th className="py-3 px-4 text-right">Triage Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-xs">
                {alertsList.map((alt) => (
                  <tr
                    key={alt.id}
                    className="hover:bg-slate-900/50 transition cursor-pointer group"
                    onClick={() => setSelectedAlert(alt)}
                  >
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <SeverityBadge severity={alt.severity} size="sm" />
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <StatusPill status={alt.status} />
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-200 group-hover:text-blue-400 transition">
                        {alt.title}
                      </div>
                      <div className="text-[11px] font-mono text-slate-400 mt-0.5">
                        {alt.ruleKey} • v{alt.ruleVersion}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap font-mono text-xs">
                      <span className="inline-flex items-center px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300">
                        <Layers className="w-3 h-3 text-blue-400 mr-1.5" />
                        {alt.eventCount} {alt.eventCount === 1 ? 'event' : 'events'}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap font-mono text-[11px] text-slate-400">
                      <div>Last seen: {new Date(alt.lastSeenAt).toLocaleTimeString([], { hour12: false })}</div>
                      <div className="text-[10px] text-slate-400">
                        First: {new Date(alt.firstSeenAt).toLocaleTimeString([], { hour12: false })}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end space-x-1.5">
                        {alt.status === 'OPEN' && (
                          <button
                            onClick={(e) => handleInlineStatusChange(e, alt.id, 'INVESTIGATING')}
                            className="px-2 py-1 rounded bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 text-[10px] font-mono transition"
                          >
                            Investigate
                          </button>
                        )}

                        {alt.status === 'INVESTIGATING' && (
                          <button
                            onClick={(e) => handleInlineStatusChange(e, alt.id, 'RESOLVED')}
                            className="px-2 py-1 rounded bg-blue-500/10 hover:bg-blue-500/20 text-blue-300 border border-blue-500/30 text-[10px] font-mono transition"
                          >
                            Resolve
                          </button>
                        )}

                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedAlert(alt);
                          }}
                          className="p-1 rounded bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-white border border-slate-800 transition"
                          title="Open alert details"
                        >
                          <ChevronRight className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Server-Side Pagination Bar */}
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          onPageChange={(page) => setCurrentPage(page)}
          onPageSizeChange={(size) => {
            setPageSize(size);
            setCurrentPage(0);
          }}
          disabled={isLoading}
        />
      </div>

      {/* Alert Detail Modal */}
      <AlertDetailModal
        alert={selectedAlert}
        onClose={() => setSelectedAlert(null)}
        onAlertUpdated={handleAlertUpdated}
        onNavigateToIncidents={onNavigateToIncidents}
      />
    </div>
  );
};
