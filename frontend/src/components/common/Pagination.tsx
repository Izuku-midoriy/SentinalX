import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (newPage: number) => void;
  onPageSizeChange?: (newSize: number) => void;
  disabled?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  onPageSizeChange,
  disabled = false,
}) => {
  const startItem = totalElements === 0 ? 0 : currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 bg-[#0c1220]/80 border-t border-slate-800/80 text-xs text-slate-400">
      <div className="flex items-center space-x-2 font-mono">
        <span>
          Showing <strong className="text-slate-200">{startItem}</strong>–<strong className="text-slate-200">{endItem}</strong> of{' '}
          <strong className="text-slate-200">{totalElements}</strong> items
        </span>

        {onPageSizeChange && (
          <div className="flex items-center space-x-1.5 ml-4 pl-4 border-l border-slate-800">
            <span>Per page:</span>
            <select
              value={pageSize}
              disabled={disabled}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="bg-slate-900 border border-slate-700/80 rounded px-2 py-0.5 text-slate-200 focus:outline-none focus:border-blue-500 font-mono text-xs cursor-pointer"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </div>
        )}
      </div>

      <div className="flex items-center space-x-2">
        <span className="font-mono text-xs text-slate-400">
          Page <strong className="text-slate-200">{currentPage + 1}</strong> of <strong className="text-slate-200">{Math.max(1, totalPages)}</strong>
        </span>

        <div className="flex items-center space-x-1 ml-2">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage <= 0 || disabled}
            aria-label="Previous page"
            className="p-1.5 rounded-md bg-slate-900 hover:bg-slate-800 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed border border-slate-800 transition"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1 || disabled}
            aria-label="Next page"
            className="p-1.5 rounded-md bg-slate-900 hover:bg-slate-800 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed border border-slate-800 transition"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
