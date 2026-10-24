import React, { useState } from 'react';
import { useAuth, PRESET_ACCOUNTS } from '../../context/AuthContext';
import { Users, X, Shield, KeyRound, Check, AlertCircle, UserPlus, Building2 } from 'lucide-react';
import { UserRole } from '../../types/auth';

interface TenantSwitcherModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const TenantSwitcherModal: React.FC<TenantSwitcherModalProps> = ({ isOpen, onClose }) => {
  const { user, quickSwitch, login, register, isLoading, error } = useAuth();
  const [showCustom, setShowCustom] = useState(false);
  const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');

  // Custom login state
  const [customEmail, setCustomEmail] = useState('');
  const [customPassword, setCustomPassword] = useState('');

  // Register profile state
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regTenant, setRegTenant] = useState('alpha-lab');
  const [regRole, setRegRole] = useState<UserRole>('ANALYST');

  const [localError, setLocalError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handlePresetSelect = async (key: 'alpha' | 'beta' | 'admin') => {
    setLocalError(null);
    try {
      await quickSwitch(key);
      onClose();
    } catch (err: unknown) {
      setLocalError(err instanceof Error ? err.message : 'Switch failed');
    }
  };

  const handleCustomLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    try {
      await login(customEmail, customPassword);
      onClose();
    } catch (err: unknown) {
      setLocalError(err instanceof Error ? err.message : 'Login failed');
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    try {
      await register({
        displayName: regName.trim(),
        email: regEmail.trim(),
        password: regPassword,
        tenantSlug: regTenant,
        role: regRole,
      });
      onClose();
    } catch (err: unknown) {
      setLocalError(err instanceof Error ? err.message : 'Registration failed');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
      <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-[#0c1220] p-6 shadow-2xl space-y-5 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 rounded-lg bg-blue-600/20 text-blue-400 border border-blue-500/30">
              <Users className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white tracking-tight">Tenant & Identity Switcher</h3>
              <p className="text-[11px] text-slate-400">Multi-tenant isolation & RBAC testing (SX-0505)</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200 p-1 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {(error || localError) && (
          <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/25 text-rose-300 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span className="truncate">{localError || error}</span>
          </div>
        )}

        {/* Preset Test Accounts */}
        <div className="space-y-2.5">
          <label className="text-[11px] font-mono uppercase tracking-wider text-slate-400 font-semibold">
            One-Click Test Accounts
          </label>

          {(Object.entries(PRESET_ACCOUNTS) as ['alpha' | 'beta' | 'admin', typeof PRESET_ACCOUNTS['alpha']][]).map(([key, acc]) => {
            const isCurrent = user?.email === acc.email;
            return (
              <button
                key={key}
                type="button"
                disabled={isLoading}
                onClick={() => handlePresetSelect(key)}
                className={`w-full text-left p-3 rounded-xl border transition flex items-center justify-between ${isCurrent
                    ? 'bg-blue-600/15 border-blue-500/40 text-white shadow-sm'
                    : 'bg-slate-900/60 hover:bg-slate-800/70 border-slate-800/80 text-slate-300'
                  }`}
              >
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-lg border ${key === 'beta'
                      ? 'bg-purple-500/15 border-purple-500/30 text-purple-400'
                      : 'bg-blue-500/15 border-blue-500/30 text-blue-400'
                    }`}>
                    <Shield className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-white flex items-center space-x-2">
                      <span>{acc.label}</span>
                      <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-800 text-slate-400">
                        {acc.tenantSlug}
                      </span>
                    </div>
                    <div className="text-[11px] font-mono text-slate-400 mt-0.5">{acc.email}</div>
                  </div>
                </div>

                {isCurrent && (
                  <span className="flex items-center text-[11px] text-blue-400 font-medium space-x-1">
                    <Check className="w-3.5 h-3.5" />
                    <span>Active</span>
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Custom Credentials & Registration Collapsible */}
        <div className="border-t border-slate-800/80 pt-4">
          <button
            type="button"
            onClick={() => setShowCustom(!showCustom)}
            className="text-xs text-blue-400 hover:text-blue-300 font-medium flex items-center space-x-1.5"
          >
            {showCustom ? <X className="w-3.5 h-3.5" /> : <KeyRound className="w-3.5 h-3.5" />}
            <span>{showCustom ? 'Hide custom accounts & registration' : 'Custom credentials or Create new profile'}</span>
          </button>

          {showCustom && (
            <div className="mt-4 space-y-4">
              {/* Tab Selector */}
              <div className="grid grid-cols-2 p-1 bg-slate-900/80 border border-slate-800 rounded-lg text-xs font-medium">
                <button
                  type="button"
                  onClick={() => setActiveTab('login')}
                  className={`py-1.5 rounded-md transition flex items-center justify-center space-x-1.5 ${activeTab === 'login'
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                    }`}
                >
                  <KeyRound className="w-3.5 h-3.5" />
                  <span>Sign In</span>
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('register')}
                  className={`py-1.5 rounded-md transition flex items-center justify-center space-x-1.5 ${activeTab === 'register'
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                    }`}
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  <span>Create Profile</span>
                </button>
              </div>

              {/* Login Form */}
              {activeTab === 'login' ? (
                <form onSubmit={handleCustomLogin} className="space-y-3">
                  <div>
                    <label className="block text-[11px] font-mono text-slate-400 mb-1">Email</label>
                    <input
                      type="email"
                      required
                      value={customEmail}
                      onChange={(e) => setCustomEmail(e.target.value)}
                      placeholder="analyst@tenant.local"
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-mono text-slate-400 mb-1">Password</label>
                    <input
                      type="password"
                      required
                      value={customPassword}
                      onChange={(e) => setCustomPassword(e.target.value)}
                      placeholder="••••••••••••"
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-medium text-xs transition disabled:opacity-50"
                  >
                    {isLoading ? 'Authenticating...' : 'Sign In'}
                  </button>
                </form>
              ) : (
                /* Registration Form */
                <form onSubmit={handleRegister} className="space-y-3">
                  <div>
                    <label className="block text-[11px] font-mono text-slate-400 mb-1">Full Name / Display Name</label>
                    <input
                      type="text"
                      required
                      value={regName}
                      onChange={(e) => setRegName(e.target.value)}
                      placeholder="e.g. Kunal Bhoir"
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-mono text-slate-400 mb-1">Email</label>
                    <input
                      type="email"
                      required
                      value={regEmail}
                      onChange={(e) => setRegEmail(e.target.value)}
                      placeholder="kunal@alpha.local"
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-mono text-slate-400 mb-1">Password (min. 6 characters)</label>
                    <input
                      type="password"
                      required
                      minLength={6}
                      value={regPassword}
                      onChange={(e) => setRegPassword(e.target.value)}
                      placeholder="••••••••••••"
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[11px] font-mono text-slate-400 mb-1 flex items-center space-x-1">
                        <Building2 className="w-3 h-3" />
                        <span>Tenant</span>
                      </label>
                      <select
                        value={regTenant}
                        onChange={(e) => setRegTenant(e.target.value)}
                        className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                      >
                        <option value="alpha-lab">Alpha Defense Lab</option>
                        <option value="beta-ops">Beta Security Ops</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-[11px] font-mono text-slate-400 mb-1 flex items-center space-x-1">
                        <Shield className="w-3 h-3" />
                        <span>Role</span>
                      </label>
                      <select
                        value={regRole}
                        onChange={(e) => setRegRole(e.target.value as UserRole)}
                        className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                      >
                        <option value="ANALYST">Analyst</option>
                        <option value="TENANT_ADMIN">Tenant Admin</option>
                      </select>
                    </div>
                  </div>
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs transition disabled:opacity-50 flex items-center justify-center space-x-1.5"
                  >
                    <UserPlus className="w-3.5 h-3.5" />
                    <span>{isLoading ? 'Creating Profile...' : 'Create Profile & Sign In'}</span>
                  </button>
                </form>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
