import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorBannerProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({
  title = 'API Request Error',
  message,
  onRetry,
}) => {
  return (
    <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/25 text-rose-300 flex items-start justify-between gap-4 my-4">
      <div className="flex items-start space-x-3">
        <div className="p-2 rounded-lg bg-rose-500/20 text-rose-400 flex-shrink-0 mt-0.5">
          <AlertTriangle className="w-4 h-4" />
        </div>
        <div>
          <h4 className="text-xs font-semibold text-rose-200">{title}</h4>
          <p className="text-xs text-rose-300/90 mt-0.5 font-mono">{message}</p>
        </div>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="px-3 py-1.5 rounded-lg bg-rose-500/20 hover:bg-rose-500/30 text-rose-200 border border-rose-500/30 text-xs font-medium flex items-center space-x-1.5 flex-shrink-0 transition"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Retry</span>
        </button>
      )}
    </div>
  );
};
