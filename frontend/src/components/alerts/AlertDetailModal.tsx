import React, { useState } from 'react';
import { AlertResponse, AlertStatus } from '../../types/alerts';
import { SecurityEventResponse } from '../../types/events';
import { updateAlert } from '../../services/alertService';
import { getEventById } from '../../services/eventService';
import { createIncidentFromAlert } from '../../services/incidentService';
import { SeverityBadge, StatusPill } from '../common/Badge';
import { EventDetailModal } from '../events/EventDetailModal';
import { X, FileText, ArrowRight, RefreshCw, AlertCircle, ShieldAlert, CheckCircle2 } from 'lucide-react';

interface AlertDetailModalProps {
  alert: AlertResponse | null;
  onClose: () => void;
  onAlertUpdated: (updated: AlertResponse) => void;
  onNavigateToIncidents?: () => void;
}

export const AlertDetailModal: React.FC<AlertDetailModalProps> = ({
  alert,
  onClose,
  onAlertUpdated,
  onNavigateToIncidents,
}) => {
  const [isUpdating, setIsUpdating] = useState(false);
  const [isEscalating, setIsEscalating] = useState(false);
  const [escalateSuccess, setEscalateSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<SecurityEventResponse | null>(null);
  const [isLoadingEvent, setIsLoadingEvent] = useState(false);

  if (!alert) return null;

  const handleStatusTransition = async (newStatus: AlertStatus) => {
    setIsUpdating(true);
    setActionError(null);
    try {
      const updated = await updateAlert(alert.id, { status: newStatus });
      onAlertUpdated(updated);
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : 'Failed to update alert lifecycle');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleEscalateToIncident = async () => {
    setIsEscalating(true);
    setActionError(null);
    setEscalateSuccess(null);
    try {
      const incident = await createIncidentFromAlert(alert.id);
      setEscalateSuccess(`Incident created successfully (ID: ${incident.id.slice(0, 8)}...)`);
      // Update status to INVESTIGATING if OPEN
      if (alert.status === 'OPEN') {
        const updated = await updateAlert(alert.id, { status: 'INVESTIGATING' });
        onAlertUpdated(updated);
      }
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : 'Failed to escalate alert to incident');
    } finally {
      setIsEscalating(false);
    }
  };

  const handleInspectSupportingEvent = async (eventId: string) => {
    setIsLoadingEvent(true);
    try {
      const evt = await getEventById(eventId);
      setSelectedEvent(evt);
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : `Failed to load supporting event ${eventId}`);
    } finally {
      setIsLoadingEvent(false);
    }
  };

  const confidencePercent = Math.round((alert.confidence || 0.9) * 100);

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
        <div className="w-full max-w-2xl max-h-[90vh] flex flex-col rounded-2xl border border-slate-800 bg-[#0c1220] shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between bg-slate-900/40">
            <div className="flex items-center space-x-3">
              <SeverityBadge severity={alert.severity} />
              <StatusPill status={alert.status} />
              <div>
                <h3 className="text-sm font-bold text-white tracking-tight">{alert.title}</h3>
                <span className="text-[11px] font-mono text-slate-400">
                  {alert.ruleKey} • v{alert.ruleVersion}
                </span>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Scrollable Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-5 text-xs">
            {actionError && (
              <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/25 text-rose-300 text-xs flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{actionError}</span>
              </div>
            )}

            {escalateSuccess && (
              <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/25 text-emerald-300 text-xs flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <CheckCircle2 className="w-4 h-4 flex-shrink-0 text-emerald-400" />
                  <span>{escalateSuccess}</span>
                </div>
                {onNavigateToIncidents && (
                  <button
                    onClick={() => {
                      onClose();
                      onNavigateToIncidents();
                    }}
                    className="underline hover:text-emerald-200 font-semibold text-[11px]"
                  >
                    View Incidents &rarr;
                  </button>
                )}
              </div>
            )}

            {/* Description Banner */}
            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-200 leading-relaxed">
              <span className="text-slate-400 text-[10px] uppercase font-mono tracking-wider block mb-1">
                Detection Evidence Summary
              </span>
              {alert.description}
            </div>

            {/* Lifecycle Quick Actions (SX-0404 & SX-0601) */}
            <div className="p-4 rounded-xl border border-slate-800/90 bg-gradient-to-r from-slate-900/90 to-slate-900/40 space-y-2.5">
              <span className="text-[11px] font-mono uppercase tracking-wider text-slate-400 font-semibold block">
                Alert Lifecycle & Escalation
              </span>

              <div className="flex flex-wrap items-center gap-2">
                {alert.status !== 'INVESTIGATING' && (
                  <button
                    disabled={isUpdating || isEscalating}
                    onClick={() => handleStatusTransition('INVESTIGATING')}
                    className="px-3 py-1.5 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30 text-xs font-medium transition disabled:opacity-40"
                  >
                    Mark Investigating
                  </button>
                )}

                {alert.status !== 'RESOLVED' && (
                  <button
                    disabled={isUpdating || isEscalating}
                    onClick={() => handleStatusTransition('RESOLVED')}
                    className="px-3 py-1.5 rounded-lg bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 text-xs font-medium transition disabled:opacity-40"
                  >
                    Resolve Alert
                  </button>
                )}

                {alert.status !== 'CLOSED' && (
                  <button
                    disabled={isUpdating || isEscalating}
                    onClick={() => handleStatusTransition('CLOSED')}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 text-xs font-medium transition disabled:opacity-40"
                  >
                    Close Alert
                  </button>
                )}

                {alert.status !== 'OPEN' && (
                  <button
                    disabled={isUpdating || isEscalating}
                    onClick={() => handleStatusTransition('OPEN')}
                    className="px-3 py-1.5 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30 text-xs font-medium transition disabled:opacity-40"
                  >
                    Reopen
                  </button>
                )}

                {/* Escalate to Incident (SX-0601) */}
                <button
                  disabled={isUpdating || isEscalating}
                  onClick={handleEscalateToIncident}
                  className="px-3 py-1.5 rounded-lg bg-indigo-600/30 hover:bg-indigo-600/50 text-indigo-300 border border-indigo-500/40 text-xs font-semibold transition disabled:opacity-40 flex items-center gap-1.5 ml-auto"
                >
                  <ShieldAlert className="w-3.5 h-3.5 text-indigo-400" />
                  {isEscalating ? 'Escalating...' : 'Escalate to Incident'}
                </button>

                {isUpdating && (
                  <span className="text-slate-400 text-xs flex items-center space-x-1 font-mono ml-2">
                    <RefreshCw className="w-3 h-3 animate-spin" />
                    <span>Updating...</span>
                  </span>
                )}
              </div>
            </div>

            {/* Attributes Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">Total Linked Events</span>
                <span className="text-slate-200 font-bold font-mono text-sm block mt-0.5">
                  {alert.eventCount} {alert.eventCount === 1 ? 'event' : 'events'}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">Detection Confidence</span>
                <span className="text-emerald-400 font-bold font-mono text-sm block mt-0.5">
                  {confidencePercent}%
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">First Seen</span>
                <span className="text-slate-300 font-mono text-[11px] truncate block mt-0.5">
                  {new Date(alert.firstSeenAt).toLocaleString()}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">Last Seen</span>
                <span className="text-slate-300 font-mono text-[11px] truncate block mt-0.5">
                  {new Date(alert.lastSeenAt).toLocaleString()}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80 col-span-2">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">Deduplication Key</span>
                <span className="text-slate-300 font-mono text-[11px] truncate block mt-0.5">
                  {alert.deduplicationKey}
                </span>
              </div>
            </div>

            {/* Supporting Events Section */}
            <div className="space-y-2.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-1.5 text-slate-400 text-xs font-mono">
                  <FileText className="w-3.5 h-3.5" />
                  <span>Linked Supporting Evidence ({alert.supportingEventIds?.length || 0} Events)</span>
                </div>
                {isLoadingEvent && <span className="text-blue-400 text-[11px] font-mono animate-pulse">Loading event telemetry...</span>}
              </div>

              <div className="divide-y divide-slate-800/80 rounded-xl border border-slate-800 bg-[#070a10] max-h-48 overflow-y-auto font-mono text-xs">
                {alert.supportingEventIds?.length ? (
                  alert.supportingEventIds.map((evtId, idx) => (
                    <div
                      key={evtId}
                      className="p-2.5 px-3 flex items-center justify-between hover:bg-slate-900/60 transition"
                    >
                      <div className="flex items-center space-x-2">
                        <span className="text-slate-400 text-[11px]">#{idx + 1}</span>
                        <span className="text-slate-200 text-[11px]">{evtId}</span>
                      </div>
                      <button
                        onClick={() => handleInspectSupportingEvent(evtId)}
                        className="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-[10px] transition flex items-center space-x-1"
                      >
                        <span>View Telemetry</span>
                        <ArrowRight className="w-2.5 h-2.5" />
                      </button>
                    </div>
                  ))
                ) : (
                  <div className="p-4 text-center text-slate-400 text-xs">
                    No individual event IDs linked.
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 py-3 border-t border-slate-800/80 bg-slate-900/40 flex items-center justify-between text-xs text-slate-400 font-mono">
            <span>Alert ID: {alert.id}</span>
            <button
              onClick={onClose}
              className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
            >
              Close
            </button>
          </div>
        </div>
      </div>

      {/* Supporting Event Inspector Sub-modal */}
      <EventDetailModal
        event={selectedEvent}
        onClose={() => setSelectedEvent(null)}
      />
    </>
  );
};
