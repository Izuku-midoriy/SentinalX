import React, { useState, useEffect, useCallback } from 'react';
import {
  IncidentResponse,
  IncidentStatus,
  IncidentSeverity,
} from '../../types/incident';
import { getTotalElements, getTotalPages } from '../../types/pagination';
import { searchIncidents, createIncident } from '../../services/incidentService';
import { IncidentDetailModal } from './IncidentDetailModal';
import {
  ShieldAlert,
  Filter,
  Plus,
  RefreshCw,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  User,
  ExternalLink,
} from 'lucide-react';

export const IncidentsView: React.FC = () => {
  const [incidents, setIncidents] = useState<IncidentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [statusFilter, setStatusFilter] = useState<IncidentStatus | 'ALL'>('ALL');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Selected incident for modal
  const [selectedIncident, setSelectedIncident] = useState<IncidentResponse | null>(null);

  // Create Incident Modal State
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newSeverity, setNewSeverity] = useState<IncidentSeverity>('HIGH');
  const [newAssignee, setNewAssignee] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const fetchIncidents = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await searchIncidents({
        status: statusFilter,
        severity: severityFilter,
        page,
        size: 15,
      });
      setIncidents(data.content);
      setTotalPages(getTotalPages(data));
      setTotalElements(getTotalElements(data));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to fetch incidents');
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter, severityFilter, page]);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents]);

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    setIsCreating(true);
    try {
      await createIncident({
        title: newTitle.trim(),
        description: newDesc.trim() || undefined,
        severity: newSeverity,
        assignedToEmail: newAssignee.trim() || undefined,
      });
      setIsCreateOpen(false);
      setNewTitle('');
      setNewDesc('');
      setNewAssignee('');
      fetchIncidents();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to create incident');
    } finally {
      setIsCreating(false);
    }
  };

  const getSeverityBadge = (sev: string) => {
    switch (sev.toUpperCase()) {
      case 'CRITICAL':
        return 'bg-red-950/80 text-red-400 border-red-700/80';
      case 'HIGH':
        return 'bg-orange-950/80 text-orange-400 border-orange-700/80';
      case 'MEDIUM':
        return 'bg-amber-950/80 text-amber-400 border-amber-700/80';
      case 'LOW':
        return 'bg-blue-950/80 text-blue-400 border-blue-700/80';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const getStatusBadge = (st: IncidentStatus) => {
    switch (st) {
      case 'OPEN':
        return 'bg-red-500/10 text-red-400 border-red-500/30';
      case 'INVESTIGATING':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'CONTAINED':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/30';
      case 'RESOLVED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'CLOSED':
        return 'bg-slate-800 text-slate-400 border-slate-700';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <ShieldAlert className="w-6 h-6 text-indigo-400" />
            Security Incident Workspace
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Track, assign, investigate, and remediate elevated security incidents with complete audit trails.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <button
            onClick={() => setIsCreateOpen(true)}
            className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            New Incident
          </button>
          <button
            onClick={fetchIncidents}
            disabled={isLoading}
            className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-semibold transition border border-slate-700/80"
            title="Refresh Incidents"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-800 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3 text-xs">
          <span className="text-slate-400 font-semibold flex items-center gap-1.5">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            Status:
          </span>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as IncidentStatus | 'ALL');
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 text-slate-200 rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-indigo-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="OPEN">OPEN</option>
            <option value="INVESTIGATING">INVESTIGATING</option>
            <option value="CONTAINED">CONTAINED</option>
            <option value="RESOLVED">RESOLVED</option>
            <option value="CLOSED">CLOSED</option>
          </select>

          <span className="text-slate-400 font-semibold flex items-center gap-1.5 ml-2">
            Severity:
          </span>
          <select
            value={severityFilter}
            onChange={(e) => {
              setSeverityFilter(e.target.value);
              setPage(0);
            }}
            className="bg-slate-800 border border-slate-700 text-slate-200 rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-indigo-500"
          >
            <option value="ALL">All Severities</option>
            <option value="CRITICAL">CRITICAL</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>
        </div>

        <div className="text-xs text-slate-500 font-mono">
          Total: <span className="text-slate-300 font-bold">{totalElements}</span> Incidents
        </div>
      </div>

      {/* Main Table Content */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
        {isLoading ? (
          <div className="py-20 text-center text-slate-400 flex items-center justify-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-indigo-400" />
            Loading security incidents...
          </div>
        ) : error ? (
          <div className="py-16 text-center text-red-400 flex flex-col items-center gap-2">
            <AlertCircle className="w-8 h-8 text-red-400" />
            <p className="text-sm font-semibold">{error}</p>
            <button
              onClick={fetchIncidents}
              className="text-xs text-indigo-400 underline hover:text-indigo-300 mt-2"
            >
              Retry
            </button>
          </div>
        ) : incidents.length === 0 ? (
          <div className="py-16 text-center text-slate-500 space-y-2">
            <ShieldAlert className="w-10 h-10 text-slate-600 mx-auto" />
            <p className="text-sm font-medium">No incidents found matching current filters.</p>
            <p className="text-xs text-slate-600">Escalate an alert or click 'New Incident' to start an investigation.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900 text-slate-400 uppercase font-mono tracking-wider border-b border-slate-800 text-[11px]">
                <tr>
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Incident Title</th>
                  <th className="py-3 px-4">Assignee</th>
                  <th className="py-3 px-4">Created By</th>
                  <th className="py-3 px-4">Last Updated</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {incidents.map((inc) => (
                  <tr
                    key={inc.id}
                    onClick={() => setSelectedIncident(inc)}
                    className="hover:bg-slate-800/50 transition cursor-pointer group"
                  >
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className={`px-2 py-0.5 rounded-full font-bold border text-[11px] ${getSeverityBadge(inc.severity)}`}>
                        {inc.severity}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className={`px-2 py-0.5 rounded-full font-mono text-[11px] border ${getStatusBadge(inc.status)}`}>
                        {inc.status}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-slate-100 group-hover:text-indigo-400 transition">
                      {inc.title}
                      {inc.linkedAlertIds.length > 0 && (
                        <span className="ml-2 text-[10px] text-amber-400/90 font-mono bg-amber-950/60 px-1.5 py-0.5 rounded border border-amber-800/60">
                          {inc.linkedAlertIds.length} Alert{inc.linkedAlertIds.length > 1 ? 's' : ''}
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap font-mono text-slate-300">
                      {inc.assignedToEmail ? (
                        <span className="flex items-center gap-1.5">
                          <User className="w-3 h-3 text-indigo-400" />
                          {inc.assignedToEmail}
                        </span>
                      ) : (
                        <span className="text-slate-600 italic">Unassigned</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap text-slate-400 font-mono text-[11px]">
                      {inc.createdByEmail}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap font-mono text-slate-500 text-[11px]">
                      {new Date(inc.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                    </td>
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedIncident(inc);
                        }}
                        className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs font-semibold inline-flex items-center gap-1 border border-slate-700 transition"
                      >
                        Investigate
                        <ExternalLink className="w-3 h-3" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="px-4 py-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400 bg-slate-900/40">
            <span>
              Page {page + 1} of {totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-1 rounded bg-slate-800 text-slate-300 hover:bg-slate-700 disabled:opacity-40 transition"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="p-1 rounded bg-slate-800 text-slate-300 hover:bg-slate-700 disabled:opacity-40 transition"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Manual Create Incident Modal */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="bg-[#0f172a] border border-slate-700 rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-indigo-400" />
              Create Security Incident
            </h2>

            <form onSubmit={handleCreateSubmit} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="text-slate-400 font-semibold uppercase">Incident Title *</label>
                <input
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Distributed Credential Stuffing Campaign"
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-slate-400 font-semibold uppercase">Severity</label>
                  <select
                    value={newSeverity}
                    onChange={(e) => setNewSeverity(e.target.value as IncidentSeverity)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-200 focus:outline-none focus:border-indigo-500"
                  >
                    <option value="CRITICAL">CRITICAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LOW">LOW</option>
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-slate-400 font-semibold uppercase">Assignee Email</label>
                  <input
                    type="text"
                    value={newAssignee}
                    onChange={(e) => setNewAssignee(e.target.value)}
                    placeholder="analyst@alpha.local"
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-200 focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-slate-400 font-semibold uppercase">Initial Investigation Scope</label>
                <textarea
                  rows={3}
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  placeholder="Provide context or hypotheses regarding this security incident..."
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isCreating || !newTitle.trim()}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-semibold transition disabled:opacity-50 flex items-center gap-1.5"
                >
                  {isCreating && <RefreshCw className="w-4 h-4 animate-spin" />}
                  Open Incident
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Incident Detail Modal */}
      {selectedIncident && (
        <IncidentDetailModal
          incident={selectedIncident}
          onClose={() => setSelectedIncident(null)}
          onIncidentUpdated={(updated) => {
            setSelectedIncident(updated);
            setIncidents((prev) =>
              prev.map((i) => (i.id === updated.id ? updated : i))
            );
          }}
        />
      )}
    </div>
  );
};
