import React, { useState, useEffect } from 'react';
import {
  IncidentResponse,
  IncidentStatus,
  IncidentNoteResponse,
  IncidentTimelineResponse,
} from '../../types/incident';
import {
  updateIncident,
  addIncidentNote,
  getIncidentNotes,
  getIncidentTimeline,
} from '../../services/incidentService';
import {
  X,
  ShieldAlert,
  User,
  MessageSquare,
  History,
  Send,
  AlertCircle,
  CheckCircle2,
  Bell,
  RefreshCw,
  Sparkles,
  Cpu,
  Zap,
} from 'lucide-react';
import { aiService, AiStatusResponse, IncidentSummaryResponse, FlowScoreResponse } from '../../services/aiService';


interface IncidentDetailModalProps {
  incident: IncidentResponse | null;
  onClose: () => void;
  onIncidentUpdated: (updated: IncidentResponse) => void;
}

export const IncidentDetailModal: React.FC<IncidentDetailModalProps> = ({
  incident,
  onClose,
  onIncidentUpdated,
}) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'notes' | 'timeline' | 'ai'>('overview');
  const [status, setStatus] = useState<IncidentStatus>(incident?.status || 'OPEN');
  const [assignedEmail, setAssignedEmail] = useState(incident?.assignedToEmail || '');
  const [isUpdating, setIsUpdating] = useState(false);
  const [updateError, setUpdateError] = useState<string | null>(null);

  // Notes state
  const [notes, setNotes] = useState<IncidentNoteResponse[]>([]);
  const [newNote, setNewNote] = useState('');
  const [isSubmittingNote, setIsSubmittingNote] = useState(false);

  // Timeline state
  const [timeline, setTimeline] = useState<IncidentTimelineResponse[]>([]);
  const [isLoadingTimeline, setIsLoadingTimeline] = useState(false);

  // AI & ML state
  const [aiStatus, setAiStatus] = useState<AiStatusResponse | null>(null);
  const [aiSummary, setAiSummary] = useState<IncidentSummaryResponse | null>(null);
  const [isLoadingAiSummary, setIsLoadingAiSummary] = useState(false);
  const [flowScoreResult, setFlowScoreResult] = useState<FlowScoreResponse | null>(null);
  const [isScoringFlow, setIsScoringFlow] = useState(false);

  useEffect(() => {
    if (incident) {
      setStatus(incident.status);
      setAssignedEmail(incident.assignedToEmail || '');
      loadNotes(incident.id);
      loadTimeline(incident.id);
      loadAiStatus();
    }
  }, [incident]);

  const loadAiStatus = async () => {
    try {
      const res = await aiService.getStatus();
      setAiStatus(res);
    } catch {
      // offline status is handled gracefully
    }
  };

  const handleGenerateAiSummary = async () => {
    if (!incident) return;
    setIsLoadingAiSummary(true);
    try {
      const res = await aiService.summarizeIncident(incident.id);
      setAiSummary(res);
    } catch (err: unknown) {
      console.error('Failed to generate AI summary:', err);
    } finally {
      setIsLoadingAiSummary(false);
    }
  };

  const handleTestFlowScore = async () => {
    setIsScoringFlow(true);
    try {
      const res = await aiService.scoreFlow({
        destination_port: 80,
        flow_duration: 180,
        total_fwd_packets: 2,
        total_bwd_packets: 0,
        syn_flag_count: 1,
        ack_flag_count: 0,
        flow_bytes_per_s: 400,
        flow_packets_per_s: 10
      });
      setFlowScoreResult(res);
    } catch (err: unknown) {
      console.error('Failed to score flow:', err);
    } finally {
      setIsScoringFlow(false);
    }
  };


  const loadNotes = async (id: string) => {
    try {
      const data = await getIncidentNotes(id);
      setNotes(data);
    } catch (err: unknown) {
      console.error('Failed to load notes:', err);
    }
  };

  const loadTimeline = async (id: string) => {
    setIsLoadingTimeline(true);
    try {
      const data = await getIncidentTimeline(id);
      setTimeline(data);
    } catch (err: unknown) {
      console.error('Failed to load timeline:', err);
    } finally {
      setIsLoadingTimeline(false);
    }
  };

  if (!incident) return null;

  const handleUpdate = async () => {
    setIsUpdating(true);
    setUpdateError(null);
    try {
      const updated = await updateIncident(incident.id, {
        status,
        assignedToEmail: assignedEmail.trim() ? assignedEmail.trim() : null,
      });
      onIncidentUpdated(updated);
      loadTimeline(incident.id);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update incident';
      setUpdateError(msg);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNote.trim()) return;

    setIsSubmittingNote(true);
    try {
      const created = await addIncidentNote(incident.id, newNote.trim());
      setNotes((prev) => [created, ...prev]);
      setNewNote('');
      loadTimeline(incident.id);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to post note');
    } finally {
      setIsSubmittingNote(false);
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-[#0f172a] border border-slate-700/80 rounded-xl max-w-4xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-start justify-between bg-slate-900/60">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${getSeverityBadge(incident.severity)}`}>
                {incident.severity}
              </span>
              <span className={`text-xs px-2.5 py-0.5 rounded-full font-mono font-medium border ${getStatusBadge(incident.status)}`}>
                {incident.status}
              </span>
              <span className="text-xs text-slate-500 font-mono">
                ID: {incident.id.slice(0, 8)}...
              </span>
            </div>
            <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-indigo-400" />
              {incident.title}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-slate-800 px-6 bg-slate-900/30 gap-6 text-sm font-medium">
          <button
            onClick={() => setActiveTab('overview')}
            className={`py-3 flex items-center gap-2 border-b-2 transition ${
              activeTab === 'overview'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <ShieldAlert className="w-4 h-4" />
            Workspace & Details
          </button>
          <button
            onClick={() => setActiveTab('notes')}
            className={`py-3 flex items-center gap-2 border-b-2 transition ${
              activeTab === 'notes'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <MessageSquare className="w-4 h-4" />
            Notes ({notes.length})
          </button>
          <button
            onClick={() => setActiveTab('timeline')}
            className={`py-3 flex items-center gap-2 border-b-2 transition ${
              activeTab === 'timeline'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <History className="w-4 h-4" />
            Activity Timeline ({timeline.length})
          </button>
          <button
            onClick={() => setActiveTab('ai')}
            className={`py-3 flex items-center gap-2 border-b-2 transition ${
              activeTab === 'ai'
                ? 'border-purple-500 text-purple-400'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Sparkles className="w-4 h-4 text-purple-400" />
            AI / ML Assist
          </button>
        </div>


        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Description */}
              <div className="space-y-2">
                <h3 className="text-xs uppercase tracking-wider text-slate-400 font-semibold">Incident Overview</h3>
                <div className="bg-slate-900/60 p-4 rounded-lg border border-slate-800 text-slate-300 text-sm whitespace-pre-wrap leading-relaxed">
                  {incident.description || 'No description provided.'}
                </div>
              </div>

              {/* Status & Assignment Controls */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-900/40 p-4 rounded-lg border border-slate-800/80">
                <div className="space-y-2">
                  <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-slate-400" />
                    Lifecycle Status
                  </label>
                  <select
                    value={status}
                    onChange={(e) => setStatus(e.target.value as IncidentStatus)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:border-indigo-500"
                  >
                    <option value="OPEN">OPEN</option>
                    <option value="INVESTIGATING">INVESTIGATING</option>
                    <option value="CONTAINED">CONTAINED</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="CLOSED">CLOSED</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    Assignee Email
                  </label>
                  <input
                    type="text"
                    value={assignedEmail}
                    onChange={(e) => setAssignedEmail(e.target.value)}
                    placeholder="analyst@domain.local"
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:border-indigo-500"
                  />
                </div>

                {updateError && (
                  <div className="col-span-2 text-xs text-red-400 flex items-center gap-1.5 bg-red-950/40 p-2.5 rounded border border-red-800/60">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    {updateError}
                  </div>
                )}

                <div className="col-span-2 flex justify-end">
                  <button
                    onClick={handleUpdate}
                    disabled={isUpdating}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-semibold transition disabled:opacity-50 flex items-center gap-2"
                  >
                    {isUpdating && <RefreshCw className="w-4 h-4 animate-spin" />}
                    Save Incident Changes
                  </button>
                </div>
              </div>

              {/* Metadata Grid */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs font-mono">
                <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800">
                  <span className="text-slate-500 block mb-1">Created By</span>
                  <span className="text-slate-300 truncate block">{incident.createdByEmail}</span>
                </div>
                <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800">
                  <span className="text-slate-500 block mb-1">Created At</span>
                  <span className="text-slate-300 block">{new Date(incident.createdAt).toLocaleString()}</span>
                </div>
                <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800">
                  <span className="text-slate-500 block mb-1">Last Updated</span>
                  <span className="text-slate-300 block">{new Date(incident.updatedAt).toLocaleString()}</span>
                </div>
                <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800">
                  <span className="text-slate-500 block mb-1">Resolved At</span>
                  <span className="text-slate-300 block">
                    {incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleString() : 'Not resolved'}
                  </span>
                </div>
              </div>

              {/* Linked Alerts */}
              <div className="space-y-2">
                <h3 className="text-xs uppercase tracking-wider text-slate-400 font-semibold flex items-center gap-1.5">
                  <Bell className="w-3.5 h-3.5 text-amber-400" />
                  Linked Alerts ({incident.linkedAlertIds.length})
                </h3>
                {incident.linkedAlertIds.length === 0 ? (
                  <p className="text-xs text-slate-500 italic bg-slate-900/30 p-3 rounded border border-slate-800">
                    No alerts currently linked to this incident.
                  </p>
                ) : (
                  <div className="space-y-1.5">
                    {incident.linkedAlertIds.map((alertId) => (
                      <div
                        key={alertId}
                        className="bg-slate-900/60 p-2.5 rounded-lg border border-slate-800 flex items-center justify-between text-xs font-mono text-slate-300"
                      >
                        <span className="flex items-center gap-2">
                          <Bell className="w-3.5 h-3.5 text-amber-400" />
                          Alert ID: {alertId}
                        </span>
                        <span className="text-emerald-400 text-[11px] bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-800/60">
                          Active Link
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Notes Tab */}
          {activeTab === 'notes' && (
            <div className="space-y-6">
              {/* Note Form */}
              <form onSubmit={handleAddNote} className="space-y-2">
                <textarea
                  value={newNote}
                  onChange={(e) => setNewNote(e.target.value)}
                  placeholder="Record investigation findings, host containment steps, or evidence notes..."
                  rows={3}
                  className="w-full bg-slate-900/80 border border-slate-700 text-slate-200 text-sm rounded-lg p-3 focus:outline-none focus:border-indigo-500 font-sans"
                />
                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={isSubmittingNote || !newNote.trim()}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-semibold transition disabled:opacity-50 flex items-center gap-2"
                  >
                    <Send className="w-4 h-4" />
                    Post Investigation Note
                  </button>
                </div>
              </form>

              {/* Notes List */}
              <div className="space-y-3">
                {notes.length === 0 ? (
                  <div className="text-center py-8 text-slate-500 text-sm">
                    No investigation notes recorded yet. Add the first note above.
                  </div>
                ) : (
                  notes.map((note) => (
                    <div
                      key={note.id}
                      className="bg-slate-900/60 p-4 rounded-lg border border-slate-800 space-y-2"
                    >
                      <div className="flex items-center justify-between text-xs text-slate-400 border-b border-slate-800 pb-2">
                        <span className="font-semibold text-slate-300 flex items-center gap-1.5">
                          <User className="w-3.5 h-3.5 text-indigo-400" />
                          {note.authorEmail}
                        </span>
                        <span className="text-slate-500 font-mono">
                          {new Date(note.createdAt).toLocaleString()}
                        </span>
                      </div>
                      <p className="text-sm text-slate-200 whitespace-pre-wrap">{note.body}</p>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Timeline Tab */}
          {activeTab === 'timeline' && (
            <div className="space-y-4">
              {isLoadingTimeline ? (
                <div className="text-center py-8 text-slate-400 flex items-center justify-center gap-2">
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  Loading activity timeline...
                </div>
              ) : timeline.length === 0 ? (
                <div className="text-center py-8 text-slate-500 text-sm">
                  No activity events recorded.
                </div>
              ) : (
                <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-800">
                  {timeline.map((entry) => (
                    <div key={entry.id} className="relative">
                      {/* Timeline dot */}
                      <div className="absolute -left-6 top-1 w-2.5 h-2.5 rounded-full bg-indigo-500 border-2 border-slate-900" />
                      <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-800 space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-semibold text-indigo-400 uppercase tracking-wider">
                            {entry.eventType}
                          </span>
                          <span className="text-slate-500 font-mono">
                            {new Date(entry.timestamp).toLocaleString()}
                          </span>
                        </div>
                        <p className="text-sm font-medium text-slate-200">{entry.summary}</p>
                        {entry.details && (
                          <p className="text-xs text-slate-400 font-mono">{entry.details}</p>
                        )}
                        <span className="text-[11px] text-slate-500 block pt-1">
                          By: {entry.actorEmail || 'System'}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* AI / ML Assist Tab */}
          {activeTab === 'ai' && (
            <div className="space-y-6">
              {/* Microservice Status Bar */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800 flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className={`w-3 h-3 rounded-full ${aiStatus?.online ? 'bg-emerald-400 animate-pulse' : 'bg-rose-500'}`} />
                  <div>
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
                      <Cpu className="w-4 h-4 text-purple-400" />
                      Python AI Microservice: {aiStatus?.online ? 'Online' : 'Offline'}
                    </h4>
                    <p className="text-xs text-slate-400">
                      Model: <span className="text-slate-200 font-mono">{aiStatus?.model_name || 'sentinelx-heuristic'}</span> ({aiStatus?.model_version || 'v0.0.1'})
                      {aiStatus?.ollama_connected ? ' • Ollama LLM Active' : ' • Rule-Template Mode'}
                    </p>
                  </div>
                </div>

                <div className="text-xs text-slate-400 font-mono bg-slate-800/80 px-3 py-1.5 rounded-lg border border-slate-700">
                  Target: {aiStatus?.service_url || 'http://localhost:8000'}
                </div>
              </div>

              {/* Triage Narrative Section */}
              <div className="bg-slate-900/40 p-5 rounded-xl border border-slate-800/80 space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                      <Sparkles className="w-4 h-4 text-purple-400" />
                      Incident Triage Narrative
                    </h3>
                    <p className="text-xs text-slate-400">
                      Generates evidence-grounded summary and investigation recommendations from incident telemetry.
                    </p>
                  </div>
                  <button
                    onClick={handleGenerateAiSummary}
                    disabled={isLoadingAiSummary}
                    className="px-3.5 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-semibold transition disabled:opacity-50 flex items-center gap-2 shadow-lg shadow-purple-900/20"
                  >
                    {isLoadingAiSummary ? (
                      <>
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                        Generating Summary...
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-3.5 h-3.5" />
                        Generate AI Summary
                      </>
                    )}
                  </button>
                </div>

                {aiSummary ? (
                  <div className="space-y-4 pt-2">
                    {/* Mandatory PRD Disclaimer */}
                    <div className="bg-amber-950/30 border border-amber-800/60 p-3 rounded-lg text-xs text-amber-300 flex items-start gap-2.5">
                      <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-semibold block">AI Model Prediction (Experimental — Not Deterministic Evidence)</span>
                        <span className="text-amber-400/80 text-[11px]">{aiSummary.disclaimer}</span>
                      </div>
                    </div>

                    {/* Summary Markdown Output */}
                    <div className="bg-slate-950/70 p-4 rounded-lg border border-slate-800 text-slate-200 text-sm whitespace-pre-wrap leading-relaxed font-sans">
                      {aiSummary.summary_markdown}
                    </div>

                    {/* Suggested Actions */}
                    {aiSummary.suggested_actions && aiSummary.suggested_actions.length > 0 && (
                      <div className="space-y-2">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 block">
                          Suggested Investigation Actions
                        </span>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                          {aiSummary.suggested_actions.map((act, idx) => (
                            <div key={idx} className="bg-slate-900/80 border border-slate-800 p-2.5 rounded text-xs text-slate-300 flex items-center gap-2">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                              <span>{act}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-center py-6 border border-dashed border-slate-800 rounded-lg text-slate-500 text-xs">
                    No summary generated yet. Click &ldquo;Generate AI Summary&rdquo; to analyze this incident.
                  </div>
                )}
              </div>

              {/* Network Flow Threat Classifier Tester */}
              <div className="bg-slate-900/40 p-5 rounded-xl border border-slate-800/80 space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                      <Zap className="w-4 h-4 text-amber-400" />
                      Supervised Network Flow Risk Scorer (CIC-IDS2017)
                    </h3>
                    <p className="text-xs text-slate-400">
                      Scores live network flow features against the trained Random Forest / XGBoost model.
                    </p>
                  </div>
                  <button
                    onClick={handleTestFlowScore}
                    disabled={isScoringFlow}
                    className="px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold transition disabled:opacity-50 flex items-center gap-2"
                  >
                    {isScoringFlow ? (
                      <>
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                        Scoring Flow...
                      </>
                    ) : (
                      <>
                        <Zap className="w-3.5 h-3.5 text-amber-400" />
                        Test Sample Flow
                      </>
                    )}
                  </button>
                </div>

                {flowScoreResult && (
                  <div className="bg-slate-950/80 p-4 rounded-lg border border-slate-800 space-y-3 text-xs">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
                      <div className="flex items-center gap-2">
                        <span className="text-slate-400">Predicted Class:</span>
                        <span className={`px-2 py-0.5 rounded font-mono font-bold ${
                          flowScoreResult.is_threat ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                        }`}>
                          {flowScoreResult.prediction}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-slate-400">Confidence:</span>
                        <span className="font-mono text-slate-200">{(flowScoreResult.confidence * 100).toFixed(1)}%</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-slate-400">Calibrated Risk:</span>
                        <span className={`font-mono font-bold ${flowScoreResult.risk_score > 50 ? 'text-rose-400' : 'text-emerald-400'}`}>
                          {flowScoreResult.risk_score}/100
                        </span>
                      </div>
                    </div>

                    <p className="text-slate-300 font-sans">{flowScoreResult.explanation}</p>

                    {flowScoreResult.class_probabilities && (
                      <div className="space-y-1.5 pt-1">
                        <span className="text-[11px] text-slate-500 uppercase tracking-wider block">Class Probability Distribution</span>
                        <div className="grid grid-cols-2 md:grid-cols-5 gap-2">
                          {Object.entries(flowScoreResult.class_probabilities).map(([cls, prob]) => (
                            <div key={cls} className="bg-slate-900 p-2 rounded border border-slate-800/80">
                              <span className="text-[10px] text-slate-400 block truncate">{cls}</span>
                              <span className="text-xs font-mono font-semibold text-slate-200">{(prob * 100).toFixed(1)}%</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

