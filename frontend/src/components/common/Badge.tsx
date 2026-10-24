import React from 'react';
import { AlertStatus } from '../../types/alerts';
import { EventSeverity } from '../../types/events';

interface SeverityBadgeProps {
  severity: EventSeverity | string;
  size?: 'sm' | 'md';
}

export const SeverityBadge: React.FC<SeverityBadgeProps> = ({ severity, size = 'md' }) => {
  const norm = (severity || 'INFO').toUpperCase();
  const sizeClasses = size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-2 py-0.5 text-xs';

  switch (norm) {
    case 'CRITICAL':
      return (
        <span className={`inline-flex items-center font-mono font-semibold rounded border bg-rose-500/15 text-rose-400 border-rose-500/30 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-rose-500 mr-1.5 animate-pulse" />
          CRITICAL
        </span>
      );
    case 'HIGH':
      return (
        <span className={`inline-flex items-center font-mono font-semibold rounded border bg-amber-500/15 text-amber-400 border-amber-500/30 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400 mr-1.5" />
          HIGH
        </span>
      );
    case 'MEDIUM':
      return (
        <span className={`inline-flex items-center font-mono font-medium rounded border bg-yellow-500/10 text-yellow-300 border-yellow-500/30 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-yellow-400 mr-1.5" />
          MEDIUM
        </span>
      );
    case 'LOW':
      return (
        <span className={`inline-flex items-center font-mono font-medium rounded border bg-blue-500/10 text-blue-400 border-blue-500/30 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400 mr-1.5" />
          LOW
        </span>
      );
    default:
      return (
        <span className={`inline-flex items-center font-mono font-medium rounded border bg-slate-800 text-slate-400 border-slate-700/60 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-slate-400 mr-1.5" />
          {norm}
        </span>
      );
  }
};

interface StatusPillProps {
  status: AlertStatus | string;
}

export const StatusPill: React.FC<StatusPillProps> = ({ status }) => {
  const norm = (status || 'OPEN').toUpperCase();

  switch (norm) {
    case 'OPEN':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-semibold border bg-emerald-500/10 text-emerald-400 border-emerald-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5" />
          OPEN
        </span>
      );
    case 'INVESTIGATING':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-semibold border bg-amber-500/10 text-amber-300 border-amber-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400 mr-1.5 animate-pulse" />
          INVESTIGATING
        </span>
      );
    case 'RESOLVED':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-semibold border bg-blue-500/10 text-blue-400 border-blue-500/30">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400 mr-1.5" />
          RESOLVED
        </span>
      );
    case 'CLOSED':
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono font-medium border bg-slate-800 text-slate-400 border-slate-700">
          <span className="w-1.5 h-1.5 rounded-full bg-slate-500 mr-1.5" />
          CLOSED
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono border bg-slate-800 text-slate-300 border-slate-700">
          {norm}
        </span>
      );
  }
};
