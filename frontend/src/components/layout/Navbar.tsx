import React, { useState } from 'react';
import { Shield, RefreshCw, Users, Bell, ListFilter, Activity, Flame, ShieldAlert, Radio } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { TenantSwitcherModal } from './TenantSwitcherModal';
import { ServiceHealthState } from '../../types/health';

interface NavbarProps {
  activeTab: 'overview' | 'events' | 'alerts' | 'incidents' | 'threat-intel' | 'lab';
  onTabChange: (tab: 'overview' | 'events' | 'alerts' | 'incidents' | 'threat-intel' | 'lab') => void;
  health: ServiceHealthState;
  onRefreshHealth: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  onTabChange,
  health,
  onRefreshHealth,
}) => {
  const { user } = useAuth();
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);

  return (
    <>
      <header className="h-16 border-b border-slate-800/80 bg-[#0c1220]/95 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-40">
        {/* Brand & Tabs */}
        <div className="flex items-center space-x-6">
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onTabChange('overview')}>
            <div className="p-2 bg-gradient-to-br from-blue-600/30 to-indigo-600/20 border border-blue-500/40 rounded-xl text-blue-400 shadow-sm shadow-blue-500/10">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-extrabold text-base tracking-tight text-white">
                  SENTINEL<span className="text-blue-500">X</span>
                </span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 font-semibold">
                  SOC V1.0
                </span>
              </div>
              <p className="text-[10px] text-slate-400 font-mono tracking-tight">AI-Assisted Multi-Tenant SIEM</p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav className="hidden md:flex items-center space-x-1 pl-6 border-l border-slate-800">
            <button
              onClick={() => onTabChange('overview')}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition flex items-center space-x-1.5 ${
                activeTab === 'overview'
                  ? 'bg-slate-800 text-white shadow-sm border border-slate-700/80 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Activity className="w-3.5 h-3.5" />
              <span>Overview</span>
            </button>

            <button
              onClick={() => onTabChange('events')}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition flex items-center space-x-1.5 ${
                activeTab === 'events'
                  ? 'bg-slate-800 text-white shadow-sm border border-slate-700/80 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <ListFilter className="w-3.5 h-3.5" />
              <span>Event Stream</span>
            </button>

            <button
              onClick={() => onTabChange('alerts')}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition flex items-center space-x-1.5 ${
                activeTab === 'alerts'
                  ? 'bg-slate-800 text-white shadow-sm border border-slate-700/80 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Bell className="w-3.5 h-3.5" />
              <span>Alert Queue</span>
            </button>

            <button
              onClick={() => onTabChange('incidents')}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition flex items-center space-x-1.5 ${
                activeTab === 'incidents'
                  ? 'bg-slate-800 text-indigo-400 shadow-sm border border-slate-700/80 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <ShieldAlert className="w-3.5 h-3.5 text-indigo-400" />
              <span>Incidents</span>
            </button>

            <button
              onClick={() => onTabChange('threat-intel')}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition flex items-center space-x-1.5 ${
                activeTab === 'threat-intel'
                  ? 'bg-red-950/70 text-red-300 shadow-sm border border-red-800/80 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Radio className="w-3.5 h-3.5 text-red-400" />
              <span>Threat Intel</span>
            </button>

            <button
              onClick={() => onTabChange('lab')}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition flex items-center space-x-1.5 ${
                activeTab === 'lab'
                  ? 'bg-rose-500/15 text-rose-300 shadow-sm border border-rose-500/30 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Flame className="w-3.5 h-3.5 text-rose-400" />
              <span>Attack Simulator</span>
            </button>
          </nav>
        </div>

        {/* Right Status & Tenant Controls */}
        <div className="flex items-center space-x-3">
          {/* Health Pill */}
          <div className="hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-full bg-slate-900/90 border border-slate-800 text-xs">
            <span className="relative flex h-2 w-2">
              {health.data ? (
                <>
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                </>
              ) : health.isChecking ? (
                <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-400 animate-pulse"></span>
              ) : (
                <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-500"></span>
              )}
            </span>
            <span className="font-mono text-[11px] text-slate-300">
              {health.data ? `Backend UP (${health.latencyMs}ms)` : health.isChecking ? 'Checking API...' : 'API Offline'}
            </span>
            <button
              onClick={onRefreshHealth}
              disabled={health.isChecking}
              title="Refresh Health"
              className="ml-1 text-slate-400 hover:text-slate-200 disabled:opacity-40"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${health.isChecking ? 'animate-spin' : ''}`} />
            </button>
          </div>

          {/* Active Tenant / User Badge */}
          <button
            type="button"
            onClick={() => setIsTenantModalOpen(true)}
            className="flex items-center space-x-2 px-3 py-1.5 rounded-xl bg-slate-900/90 hover:bg-slate-800/80 border border-slate-800 text-xs text-slate-300 transition group"
            title="Switch Tenant / User (SX-0505)"
          >
            <div className={`w-2 h-2 rounded-full ${user?.tenantSlug === 'beta-ops' ? 'bg-purple-400' : 'bg-blue-400'}`} />
            <div className="text-left font-mono text-[11px] leading-tight">
              <span className="text-slate-400">Tenant: </span>
              <span className="text-white font-semibold">{user?.tenantSlug || 'loading...'}</span>
              <div className="text-[10px] text-slate-400 font-mono truncate max-w-[120px]">{user?.email || 'unauthenticated'}</div>
            </div>
            <Users className="w-3.5 h-3.5 text-slate-400 group-hover:text-slate-200 ml-1" />
          </button>
        </div>
      </header>

      <TenantSwitcherModal
        isOpen={isTenantModalOpen}
        onClose={() => setIsTenantModalOpen(false)}
      />
    </>
  );
};
