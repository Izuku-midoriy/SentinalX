import React, { useState, useEffect, useCallback } from 'react';
import { SecurityEventResponse } from '../../types/events';
import { PageResponse, getTotalElements, getTotalPages } from '../../types/pagination';
import { searchEvents } from '../../services/eventService';
import { SeverityBadge } from '../common/Badge';
import { Pagination } from '../common/Pagination';
import { TableSkeleton } from '../common/LoadingSkeleton';
import { EmptyState } from '../common/EmptyState';
import { ErrorBanner } from '../common/ErrorBanner';
import { EventDetailModal } from './EventDetailModal';
import { useAuth } from '../../context/AuthContext';
import { Search, RefreshCw, Filter, Eye, ShieldAlert, ArrowRight } from 'lucide-react';

export const EventsView: React.FC = () => {
  const { user } = useAuth();
  const [eventsPage, setEventsPage] = useState<PageResponse<SecurityEventResponse> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination State
  const [searchType, setSearchType] = useState<string>('');
  const [selectedSeverity, setSelectedSeverity] = useState<string>('ALL');
  const [selectedSourceType, setSelectedSourceType] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);

  // Selected event for modal inspection
  const [inspectedEvent, setInspectedEvent] = useState<SecurityEventResponse | null>(null);

  const loadEvents = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await searchEvents({
        eventType: searchType || undefined,
        severity: selectedSeverity !== 'ALL' ? selectedSeverity : undefined,
        sourceType: selectedSourceType !== 'ALL' ? selectedSourceType : undefined,
        page: currentPage,
        size: pageSize,
      });
      setEventsPage(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to query security events';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [searchType, selectedSeverity, selectedSourceType, currentPage, pageSize, user?.tenantSlug]);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  // Handle filter changes (resets to page 0)
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadEvents();
  };

  const handleResetFilters = () => {
    setSearchType('');
    setSelectedSeverity('ALL');
    setSelectedSourceType('ALL');
    setCurrentPage(0);
  };

  const totalElements = getTotalElements(eventsPage);
  const totalPages = getTotalPages(eventsPage);
  const eventsList = eventsPage?.content || [];

  return (
    <div className="space-y-5 animate-fadeIn">
      {/* Top Filter Bar */}
      <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-4 shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row items-center gap-3">
          {/* Text Search by Event Type */}
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
              placeholder="Filter by event type (e.g. authentication_failure, port_scan)..."
              className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition font-mono"
            />
          </div>

          {/* Severity Dropdown */}
          <div className="flex items-center space-x-2 w-full md:w-auto">
            <select
              value={selectedSeverity}
              onChange={(e) => {
                setSelectedSeverity(e.target.value);
                setCurrentPage(0);
              }}
              className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500 transition font-mono cursor-pointer w-full md:w-auto"
            >
              <option value="ALL">Severity: All</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
              <option value="INFO">Info</option>
            </select>

            {/* Source Type Dropdown */}
            <select
              value={selectedSourceType}
              onChange={(e) => {
                setSelectedSourceType(e.target.value);
                setCurrentPage(0);
              }}
              className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500 transition font-mono cursor-pointer w-full md:w-auto"
            >
              <option value="ALL">Source: All</option>
              <option value="linux">Linux</option>
              <option value="firewall">Firewall</option>
              <option value="windows">Windows</option>
              <option value="web">Web</option>
              <option value="network">Network</option>
            </select>

            <button
              type="submit"
              className="px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium transition flex items-center space-x-1.5 shadow-sm flex-shrink-0"
            >
              <Filter className="w-3.5 h-3.5" />
              <span>Filter</span>
            </button>

            <button
              type="button"
              onClick={loadEvents}
              disabled={isLoading}
              title="Refresh events"
              className="p-2 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-white transition flex-shrink-0 disabled:opacity-40"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </form>
      </div>

      {/* Error state banner */}
      {error && <ErrorBanner message={error} onRetry={loadEvents} />}

      {/* Main Events Table Card */}
      <div className="rounded-xl border border-slate-800 bg-[#0c1220] overflow-hidden shadow-sm">
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <h3 className="text-sm font-semibold text-white tracking-tight">Normalized Security Events</h3>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-800">
              Tenant: {user?.tenantSlug || 'loading'}
            </span>
          </div>
          <span className="text-xs font-mono text-slate-400">
            {totalElements} total records
          </span>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          {isLoading ? (
            <TableSkeleton rows={8} columns={6} />
          ) : eventsList.length === 0 ? (
            <EmptyState
              icon={<ShieldAlert className="w-8 h-8 text-slate-500" />}
              title="No Security Events Found"
              description={`No ingested events match your filters for tenant '${user?.tenantSlug}'. You can replay events using the Attack Simulator tab or the replay script.`}
              actionText="Reset Filters"
              onAction={handleResetFilters}
            />
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800/80 bg-slate-900/40 text-[11px] font-mono uppercase tracking-wider text-slate-400">
                  <th className="py-3 px-4">Timestamp</th>
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Event Type</th>
                  <th className="py-3 px-4">Source Host</th>
                  <th className="py-3 px-4">Network / Identity</th>
                  <th className="py-3 px-4">Message</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-xs font-mono">
                {eventsList.map((evt) => (
                  <tr
                    key={evt.id}
                    className="hover:bg-slate-900/50 transition cursor-pointer group"
                    onClick={() => setInspectedEvent(evt)}
                  >
                    <td className="py-3 px-4 text-slate-400 whitespace-nowrap text-[11px]">
                      {new Date(evt.eventTime).toLocaleTimeString([], { hour12: false })}
                      <span className="block text-[10px] text-slate-400">
                        {new Date(evt.eventTime).toISOString().slice(0, 10)}
                      </span>
                    </td>

                    <td className="py-3 px-4 whitespace-nowrap">
                      <SeverityBadge severity={evt.severity} size="sm" />
                    </td>

                    <td className="py-3 px-4 font-semibold text-slate-200 whitespace-nowrap">
                      {evt.eventType}
                    </td>

                    <td className="py-3 px-4 text-slate-300 whitespace-nowrap">
                      <div className="text-slate-200">{evt.host || evt.sourceName}</div>
                      <span className="text-[10px] text-slate-400">{evt.sourceType}</span>
                    </td>

                    <td className="py-3 px-4 text-slate-300 whitespace-nowrap">
                      {evt.sourceIp ? (
                        <div className="flex items-center space-x-1">
                          <span className="text-slate-200">{evt.sourceIp}</span>
                          {evt.destinationIp && (
                            <>
                              <ArrowRight className="w-3 h-3 text-slate-400" />
                              <span className="text-slate-400">{evt.destinationIp}</span>
                            </>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                      {evt.username && (
                        <div className="text-[10px] text-blue-400">user: {evt.username}</div>
                      )}
                    </td>

                    <td className="py-3 px-4 text-slate-400 truncate max-w-xs">
                      {evt.message || <span className="text-slate-400 font-italic">No message payload</span>}
                    </td>

                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setInspectedEvent(evt);
                        }}
                        className="px-2.5 py-1 rounded bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 text-[11px] font-sans inline-flex items-center space-x-1 transition"
                      >
                        <Eye className="w-3 h-3 text-slate-400" />
                        <span>Inspect</span>
                      </button>
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

      {/* Inspect Modal */}
      <EventDetailModal
        event={inspectedEvent}
        onClose={() => setInspectedEvent(null)}
      />
    </div>
  );
};
