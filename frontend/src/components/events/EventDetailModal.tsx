import React from 'react';
import { SecurityEventResponse } from '../../types/events';
import { SeverityBadge } from '../common/Badge';
import { X, Copy, Check, Terminal } from 'lucide-react';

interface EventDetailModalProps {
  event: SecurityEventResponse | null;
  onClose: () => void;
}

export const EventDetailModal: React.FC<EventDetailModalProps> = ({ event, onClose }) => {
  const [copied, setCopied] = React.useState(false);

  if (!event) return null;

  const handleCopyRaw = () => {
    const textToCopy = event.rawEvent || JSON.stringify(event, null, 2);
    navigator.clipboard.writeText(textToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-2xl max-h-[90vh] flex flex-col rounded-2xl border border-slate-800 bg-[#0c1220] shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between bg-slate-900/40">
          <div className="flex items-center space-x-3">
            <SeverityBadge severity={event.severity} />
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-sm font-bold text-white font-mono">{event.eventType}</h3>
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">
                  Schema v{event.schemaVersion}
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-mono mt-0.5">ID: {event.id}</p>
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
          {/* Message Banner */}
          {event.message && (
            <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 font-mono text-slate-200">
              <span className="text-slate-400 text-[10px] uppercase tracking-wider block mb-1">Normalized Message</span>
              {event.message}
            </div>
          )}

          {/* Grid of Normalized Attributes */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Source Name</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">{event.sourceName}</span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Source Type</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">{event.sourceType}</span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Host Target</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">{event.host || 'N/A'}</span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Source IP</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">{event.sourceIp || 'N/A'}</span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Destination IP</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">{event.destinationIp || 'N/A'}</span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Target Username</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">{event.username || 'N/A'}</span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Process</span>
              <span className="text-slate-200 font-medium font-mono truncate block mt-0.5">
                {event.processName ? `${event.processName} (${event.processId || '?'})` : 'N/A'}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Event Timestamp</span>
              <span className="text-slate-200 font-medium font-mono text-[11px] truncate block mt-0.5">
                {new Date(event.eventTime).toLocaleString()}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800/80">
              <span className="text-[10px] font-mono uppercase text-slate-400 block">Idempotency Key</span>
              <span className="text-slate-200 font-medium font-mono text-[10px] truncate block mt-0.5">
                {event.idempotencyKey || 'None'}
              </span>
            </div>
          </div>

          {/* Raw Event Section */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-1.5 text-slate-400 text-xs font-mono">
                <Terminal className="w-3.5 h-3.5" />
                <span>Raw Payload / Event Telemetry</span>
              </div>
              <button
                onClick={handleCopyRaw}
                className="text-[11px] font-mono text-slate-400 hover:text-white flex items-center space-x-1 p-1 rounded hover:bg-slate-800 transition"
              >
                {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                <span>{copied ? 'Copied' : 'Copy Payload'}</span>
              </button>
            </div>

            <div className="p-4 rounded-xl bg-[#070a10] border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto max-h-48">
              <pre>{event.rawEvent || JSON.stringify(event, null, 2)}</pre>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-800/80 bg-slate-900/40 flex items-center justify-between text-xs text-slate-400 font-mono">
          <span>Tenant Scoped: {event.tenantId}</span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
