import React, { useState, useEffect, useCallback } from 'react';
import {
  ThreatIntelIoc,
  ThreatIntelStats,
  IocType,
  ThreatType,
  ThreatSeverity,
  IocLookupResult,
} from '../../types/threatIntel';
import {
  fetchThreatIntelIocs,
  fetchThreatIntelStats,
  lookupIoc,
  createThreatIntelIoc,
  deleteThreatIntelIoc,
} from '../../services/threatIntelService';
import {
  ShieldAlert,
  Search,
  Plus,
  RefreshCw,
  AlertTriangle,
  Database,
  Crosshair,
  Trash2,
  CheckCircle2,
  XCircle,
  Copy,
  Check,
  Globe,
  Radio,
} from 'lucide-react';

export const ThreatIntelView: React.FC = () => {
  const [iocs, setIocs] = useState<ThreatIntelIoc[]>([]);
  const [stats, setStats] = useState<ThreatIntelStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Lookup Tool State
  const [lookupQuery, setLookupQuery] = useState('');
  const [isLookingUp, setIsLookingUp] = useState(false);
  const [lookupResult, setLookupResult] = useState<IocLookupResult | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);

  // Add Indicator Modal State
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newType, setNewType] = useState<IocType>('IP');
  const [newValue, setNewValue] = useState('');
  const [newThreatType, setNewThreatType] = useState<ThreatType>('MALWARE_C2');
  const [newSeverity, setNewSeverity] = useState<ThreatSeverity>('HIGH');
  const [newConfidence, setNewConfidence] = useState(0.95);
  const [newSourceFeed, setNewSourceFeed] = useState('Analyst Manual Entry');
  const [newDescription, setNewDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Copy feedback
  const [copiedValue, setCopiedValue] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [iocsData, statsData] = await Promise.all([
        fetchThreatIntelIocs({
          type: typeFilter,
          search: searchTerm,
          page,
          size: 15,
        }),
        fetchThreatIntelStats().catch(() => null),
      ]);
      setIocs(iocsData.content);
      setTotalPages(iocsData.totalPages);
      setTotalElements(iocsData.totalElements);
      if (statsData) setStats(statsData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load threat intelligence');
    } finally {
      setIsLoading(false);
    }
  }, [typeFilter, searchTerm, page]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleLookup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!lookupQuery.trim()) return;

    setIsLookingUp(true);
    setLookupError(null);
    setLookupResult(null);

    try {
      const res = await lookupIoc(lookupQuery.trim());
      setLookupResult(res);
    } catch (err: unknown) {
      setLookupError(err instanceof Error ? err.message : 'Lookup failed');
    } finally {
      setIsLookingUp(false);
    }
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newValue.trim()) return;

    setIsSubmitting(true);
    try {
      await createThreatIntelIoc({
        iocType: newType,
        iocValue: newValue.trim(),
        threatType: newThreatType,
        severity: newSeverity,
        confidence: newConfidence,
        sourceFeed: newSourceFeed.trim() || 'Analyst Manual Entry',
        description: newDescription.trim() || undefined,
      });
      setIsCreateOpen(false);
      setNewValue('');
      setNewDescription('');
      loadData();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to create indicator');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to deactivate this threat indicator?')) return;

    try {
      await deleteThreatIntelIoc(id);
      loadData();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to deactivate indicator');
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedValue(text);
    setTimeout(() => setCopiedValue(null), 2000);
  };

  const getSeverityBadgeClass = (severity: ThreatSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-950/80 text-red-400 border border-red-800/80';
      case 'HIGH':
        return 'bg-amber-950/80 text-amber-400 border border-amber-800/80';
      case 'MEDIUM':
        return 'bg-yellow-950/60 text-yellow-300 border border-yellow-800/60';
      case 'LOW':
        return 'bg-blue-950/60 text-blue-300 border border-blue-800/60';
      default:
        return 'bg-slate-800 text-slate-300';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              <Radio className="w-6 h-6 text-red-500 animate-pulse" />
              Threat Intelligence Feeds
            </h1>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-emerald-950/80 text-emerald-400 border border-emerald-800/70 flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
              FEED: HEALTHY
            </span>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Real-time correlation against curated community threat feeds, C2 IP blocklists, and adversary IOC signatures.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={loadData}
            disabled={isLoading}
            className="flex items-center gap-2 px-3.5 py-2 text-xs font-medium bg-slate-900 border border-slate-700/80 text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="flex items-center gap-2 px-4 py-2 text-xs font-semibold bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white rounded-lg shadow-lg shadow-red-900/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            Add Indicator
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3 bg-red-950/70 border border-red-800 text-red-300 rounded-lg text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
            <span>{error}</span>
          </div>
          <button
            onClick={loadData}
            className="text-xs font-semibold underline hover:text-white"
          >
            Retry
          </button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4 shadow-sm backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>ACTIVE INDICATORS</span>
            <ShieldAlert className="w-4 h-4 text-blue-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono text-white">
              {stats?.totalIocs ?? iocs.length}
            </span>
            <span className="text-xs text-slate-400">indexed</span>
          </div>
        </div>

        <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4 shadow-sm backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>HIGH & CRITICAL THREATS</span>
            <AlertTriangle className="w-4 h-4 text-red-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono text-red-400">
              {stats?.highCriticalIocs ?? 0}
            </span>
            <span className="text-xs text-slate-400">immediate alerts</span>
          </div>
        </div>

        <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4 shadow-sm backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>TELEMETRY MATCHES</span>
            <Crosshair className="w-4 h-4 text-purple-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono text-purple-400">
              {stats?.totalMatches ?? 0}
            </span>
            <span className="text-xs text-slate-400">hit events</span>
          </div>
        </div>

        <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4 shadow-sm backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>COMMUNITY FEEDS</span>
            <Database className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono text-emerald-400">
              {stats?.activeFeedsCount ?? 4}
            </span>
            <span className="text-xs text-slate-400">integrated</span>
          </div>
        </div>
      </div>

      {/* Quick IOC Reputation Lookup Bar */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-900/95 to-slate-900/80 border border-slate-800/90 rounded-xl p-5 shadow-md">
        <div className="max-w-3xl">
          <h2 className="text-sm font-semibold text-white flex items-center gap-2">
            <Search className="w-4 h-4 text-blue-400" />
            Quick IOC Reputation Lookup
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Test any suspicious IP, C2 domain, or SHA256 hash to query reputation against SentinelX intelligence.
          </p>

          <form onSubmit={handleLookup} className="mt-3 flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={lookupQuery}
                onChange={(e) => setLookupQuery(e.target.value)}
                placeholder="Enter IP (e.g. 198.51.100.23) or Domain (e.g. evil-c2-gateway.org)..."
                className="w-full bg-slate-950 border border-slate-700/80 rounded-lg px-3.5 py-2 text-sm text-slate-100 placeholder-slate-500 font-mono focus:outline-none focus:ring-2 focus:ring-blue-500/50"
              />
            </div>
            <button
              type="submit"
              disabled={isLookingUp || !lookupQuery.trim()}
              className="px-5 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 text-white rounded-lg text-xs font-semibold shadow transition-colors flex items-center gap-2"
            >
              {isLookingUp ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />}
              Check Reputation
            </button>
          </form>

          {/* Lookup Results */}
          {lookupResult && (
            <div className="mt-4 p-4 rounded-lg border transition-all animate-in fade-in duration-200">
              {lookupResult.matched && lookupResult.ioc ? (
                <div className="bg-red-950/40 border-red-800/80 p-4 rounded-lg border flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-2">
                      <XCircle className="w-5 h-5 text-red-400 shrink-0" />
                      <span className="text-sm font-bold text-red-200">
                        MALICIOUS THREAT DETECTED
                      </span>
                      <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold ${getSeverityBadgeClass(lookupResult.ioc.severity)}`}>
                        {lookupResult.ioc.severity}
                      </span>
                    </div>
                    <div className="text-xs text-slate-300 font-mono">
                      Target: <span className="text-white font-bold">{lookupResult.ioc.iocValue}</span> ({lookupResult.ioc.iocType})
                    </div>
                    <div className="text-xs text-slate-400">
                      Category: <span className="text-amber-300 font-semibold">{lookupResult.ioc.threatType}</span> • Feed: <span className="text-slate-200">{lookupResult.ioc.sourceFeed}</span>
                    </div>
                    {lookupResult.ioc.description && (
                      <div className="text-xs text-slate-300 bg-black/40 p-2 rounded border border-slate-800">
                        {lookupResult.ioc.description}
                      </div>
                    )}
                  </div>

                  <div className="text-right shrink-0">
                    <div className="text-xs text-slate-400 font-mono">
                      Confidence: {(lookupResult.ioc.confidence * 100).toFixed(0)}%
                    </div>
                    <div className="text-xs text-purple-300 font-mono mt-1">
                      Matched {lookupResult.ioc.matchCount} times in logs
                    </div>
                  </div>
                </div>
              ) : (
                <div className="bg-emerald-950/30 border-emerald-800/70 p-3.5 rounded-lg border flex items-center gap-3">
                  <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                  <div>
                    <div className="text-sm font-bold text-emerald-300">
                      Clean Indicator / No Known Threat
                    </div>
                    <div className="text-xs text-slate-400 mt-0.5 font-mono">
                      &quot;{lookupResult.queryValue}&quot; is not flagged in any active community or local threat feeds.
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {lookupError && (
            <div className="mt-3 p-3 bg-red-950/50 border border-red-800/60 rounded-lg text-xs text-red-300">
              Lookup error: {lookupError}
            </div>
          )}
        </div>
      </div>

      {/* Filter Tabs & Search */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="flex items-center gap-1 bg-slate-900 border border-slate-800/80 p-1 rounded-lg">
          {['ALL', 'IP', 'DOMAIN', 'HASH_SHA256'].map((type) => (
            <button
              key={type}
              onClick={() => {
                setTypeFilter(type);
                setPage(0);
              }}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                typeFilter === type
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              {type === 'HASH_SHA256' ? 'HASHES' : type}
            </button>
          ))}
        </div>

        <div className="relative flex-1 sm:max-w-xs">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
            placeholder="Search indicators, feeds, or notes..."
            className="w-full bg-slate-900/90 border border-slate-800 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Threat Indicators Table */}
      <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 text-slate-400 border-b border-slate-800/80 font-mono uppercase tracking-wider text-[11px]">
              <tr>
                <th className="py-3 px-4">Indicator Value</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Threat Category</th>
                <th className="py-3 px-4">Severity</th>
                <th className="py-3 px-4">Confidence</th>
                <th className="py-3 px-4">Source Feed</th>
                <th className="py-3 px-4 text-center">Telemetry Matches</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-sans">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto text-blue-500 mb-2" />
                    Loading threat intelligence database...
                  </td>
                </tr>
              ) : iocs.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    No threat indicators found matching current filters.
                  </td>
                </tr>
              ) : (
                iocs.map((ioc) => (
                  <tr
                    key={ioc.id}
                    className="hover:bg-slate-800/30 transition-colors group"
                  >
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-semibold text-slate-100 group-hover:text-blue-400 transition-colors">
                          {ioc.iocValue}
                        </span>
                        <button
                          onClick={() => copyToClipboard(ioc.iocValue)}
                          title="Copy indicator"
                          className="text-slate-500 hover:text-slate-300 p-1"
                        >
                          {copiedValue === ioc.iocValue ? (
                            <Check className="w-3.5 h-3.5 text-emerald-400" />
                          ) : (
                            <Copy className="w-3.5 h-3.5" />
                          )}
                        </button>
                      </div>
                      {ioc.description && (
                        <div className="text-[11px] text-slate-400 truncate max-w-sm mt-0.5">
                          {ioc.description}
                        </div>
                      )}
                    </td>

                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-slate-800 text-slate-300 border border-slate-700/60">
                        {ioc.iocType}
                      </span>
                    </td>

                    <td className="py-3 px-4 font-mono font-semibold text-slate-200">
                      {ioc.threatType}
                    </td>

                    <td className="py-3 px-4">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold font-mono ${getSeverityBadgeClass(ioc.severity)}`}>
                        {ioc.severity}
                      </span>
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-16 bg-slate-800 rounded-full h-1.5 overflow-hidden">
                          <div
                            className="bg-blue-500 h-1.5 rounded-full"
                            style={{ width: `${Math.round(ioc.confidence * 100)}%` }}
                          />
                        </div>
                        <span className="text-[11px] font-mono text-slate-400">
                          {(ioc.confidence * 100).toFixed(0)}%
                        </span>
                      </div>
                    </td>

                    <td className="py-3 px-4">
                      <div className="flex items-center gap-1.5 text-slate-300 text-xs">
                        <Globe className="w-3 h-3 text-slate-400 shrink-0" />
                        <span className="truncate max-w-[140px]">{ioc.sourceFeed}</span>
                      </div>
                    </td>

                    <td className="py-3 px-4 text-center font-mono">
                      {ioc.matchCount > 0 ? (
                        <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-purple-950/80 text-purple-300 border border-purple-800/80">
                          {ioc.matchCount} hits
                        </span>
                      ) : (
                        <span className="text-slate-500 text-xs">—</span>
                      )}
                    </td>

                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={(e) => handleDelete(ioc.id, e)}
                        title="Deactivate indicator"
                        className="text-slate-500 hover:text-red-400 p-1.5 rounded hover:bg-slate-800 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="py-3 px-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400 font-mono">
          <div>
            Showing {iocs.length} of {totalElements} indicators
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 rounded transition-colors"
            >
              Prev
            </button>
            <span>
              Page {page + 1} of {Math.max(1, totalPages)}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-200 rounded transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Add Indicator Modal */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <ShieldAlert className="w-5 h-5 text-red-500" />
                Add Threat Indicator (IOC)
              </h3>
              <button
                onClick={() => setIsCreateOpen(false)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ×
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Indicator Type
                  </label>
                  <select
                    value={newType}
                    onChange={(e) => setNewType(e.target.value as IocType)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2 text-xs text-slate-200"
                  >
                    <option value="IP">IP Address</option>
                    <option value="DOMAIN">Domain Name</option>
                    <option value="HASH_SHA256">SHA-256 Hash</option>
                    <option value="URL">URL</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Threat Category
                  </label>
                  <select
                    value={newThreatType}
                    onChange={(e) => setNewThreatType(e.target.value as ThreatType)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2 text-xs text-slate-200"
                  >
                    <option value="MALWARE_C2">Malware C2 Server</option>
                    <option value="RANSOMWARE">Ransomware Exfiltration</option>
                    <option value="PHISHING">Phishing / Credential Theft</option>
                    <option value="BOTNET">Botnet Node</option>
                    <option value="SCANNER">Automated Scanner</option>
                    <option value="APT_INFRASTRUCTURE">APT Infrastructure</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Indicator Value *
                </label>
                <input
                  type="text"
                  required
                  value={newValue}
                  onChange={(e) => setNewValue(e.target.value)}
                  placeholder={newType === 'IP' ? '198.51.100.99' : newType === 'DOMAIN' ? 'adversary-c2.com' : 'hash...'}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2 text-xs text-slate-100 font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Severity
                  </label>
                  <select
                    value={newSeverity}
                    onChange={(e) => setNewSeverity(e.target.value as ThreatSeverity)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2 text-xs text-slate-200"
                  >
                    <option value="CRITICAL">CRITICAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LOW">LOW</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Confidence ({Math.round(newConfidence * 100)}%)
                  </label>
                  <input
                    type="range"
                    min="0.5"
                    max="1.0"
                    step="0.05"
                    value={newConfidence}
                    onChange={(e) => setNewConfidence(parseFloat(e.target.value))}
                    className="w-full accent-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Source Feed / Provenance
                </label>
                <input
                  type="text"
                  value={newSourceFeed}
                  onChange={(e) => setNewSourceFeed(e.target.value)}
                  placeholder="e.g. AlienVault OTX, Internal Incident 042"
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2 text-xs text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Description / Context
                </label>
                <textarea
                  rows={2}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="Notes on adversary behavior, campaign, or evidence..."
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2 text-xs text-slate-100 resize-none"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsCreateOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || !newValue.trim()}
                  className="px-4 py-2 bg-red-600 hover:bg-red-500 disabled:bg-slate-800 text-white rounded-lg text-xs font-bold shadow"
                >
                  {isSubmitting ? 'Saving...' : 'Add Indicator'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
