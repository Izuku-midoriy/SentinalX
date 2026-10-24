import React from 'react';

interface LoadingSkeletonProps {
  rows?: number;
  columns?: number;
}

export const TableSkeleton: React.FC<LoadingSkeletonProps> = ({ rows = 5, columns = 6 }) => {
  return (
    <div className="w-full divide-y divide-slate-800/60 animate-pulse">
      {Array.from({ length: rows }).map((_, rIdx) => (
        <div key={rIdx} className="flex items-center space-x-4 px-6 py-4">
          {Array.from({ length: columns }).map((_, cIdx) => (
            <div
              key={cIdx}
              className="h-4 bg-slate-800/80 rounded"
              style={{
                width: cIdx === 0 ? '12%' : cIdx === 1 ? '16%' : cIdx === columns - 1 ? '24%' : '14%',
              }}
            />
          ))}
        </div>
      ))}
    </div>
  );
};

export const CardSkeleton: React.FC = () => {
  return (
    <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-3 animate-pulse">
      <div className="flex justify-between items-center">
        <div className="h-4 bg-slate-800 rounded w-1/3" />
        <div className="h-4 bg-slate-800 rounded w-1/6" />
      </div>
      <div className="h-8 bg-slate-800/60 rounded w-1/2 my-2" />
      <div className="h-3 bg-slate-800/40 rounded w-2/3" />
    </div>
  );
};
