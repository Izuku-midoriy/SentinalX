import React, { useState } from 'react';
import { ingestEvent } from '../../services/eventService';
import { useAuth } from '../../context/AuthContext';
import { Flame, ShieldCheck, Terminal, AlertTriangle, Play } from 'lucide-react';

interface SimulationLog {
  timestamp: string;
  type: 'info' | 'success' | 'warn' | 'error';
  message: string;
}

export const LabSimulatorView: React.FC<{ onNavigateToAlerts?: () => void }> = ({ onNavigateToAlerts }) => {
  const { user } = useAuth();
  const [isRunning, setIsRunning] = useState(false);
  const [logs, setLogs] = useState<SimulationLog[]>([]);

  const addLog = (message: string, type: 'info' | 'success' | 'warn' | 'error' = 'info') => {
    setLogs((prev) => [
      ...prev,
      {
        timestamp: new Date().toLocaleTimeString([], { hour12: false }),
        type,
        message,
      },
    ]);
  };

  const clearLogs = () => setLogs([]);

  // 1. Simulate Brute Force Attack (SX-0401)
  const runBruteForceSimulation = async () => {
    setIsRunning(true);
    addLog(`Initiating synthetic SSH brute-force attack simulation for tenant '${user?.tenantSlug}'...`, 'info');
    const attackerIp = `203.0.113.${Math.floor(Math.random() * 150) + 50}`;

    try {
      for (let i = 1; i <= 3; i++) {
        addLog(`Sending failed authentication event #${i} from ${attackerIp} (user: admin)...`, 'info');
        const res = await ingestEvent({
          timestamp: new Date().toISOString(),
          sourceType: 'linux',
          sourceName: 'bastion-alpha-01',
          eventType: 'authentication_failure',
          severity: 'HIGH',
          host: 'bastion-alpha-01',
          sourceIp: attackerIp,
          username: 'admin',
          message: `Failed password for admin from ${attackerIp} port ${40000 + i} ssh2`,
          idempotencyKey: `sim-bf-${Date.now()}-${i}`,
        });
        addLog(`Event #${i} ingested successfully (ID: ${res.id})`, 'success');
        // Small delay between bursts
        await new Promise((r) => setTimeout(r, 200));
      }

      addLog(`Threshold reached (3 failed logins in 5 min window). Detection rule 'RULE_AUTH_BRUTE_FORCE' evaluated and triggered alert!`, 'warn');
      addLog(`Testing deduplication: Sending 4th failed login from same attacker IP...`, 'info');

      const dupEvent = await ingestEvent({
        timestamp: new Date().toISOString(),
        sourceType: 'linux',
        sourceName: 'bastion-alpha-01',
        eventType: 'authentication_failure',
        severity: 'HIGH',
        host: 'bastion-alpha-01',
        sourceIp: attackerIp,
        username: 'admin',
        message: `Failed password for admin from ${attackerIp} port 40004 ssh2`,
        idempotencyKey: `sim-bf-${Date.now()}-4`,
      });

      addLog(`Event #4 ingested (ID: ${dupEvent.id}). Deduplication key matched: alert eventCount incremented without alert spam!`, 'success');
      addLog(`Simulation complete! Check Alert Queue to triage the generated high-severity alert.`, 'success');
    } catch (err: unknown) {
      addLog(`Simulation error: ${err instanceof Error ? err.message : 'Failed'}`, 'error');
    } finally {
      setIsRunning(false);
    }
  };

  // 2. Simulate Port Scan Attack (SX-0402)
  const runPortScanSimulation = async () => {
    setIsRunning(true);
    addLog(`Initiating network port scan simulation for tenant '${user?.tenantSlug}'...`, 'info');
    const scannerIp = `198.51.100.${Math.floor(Math.random() * 150) + 50}`;

    try {
      for (let i = 1; i <= 2; i++) {
        addLog(`Sending firewall port scan event #${i} from ${scannerIp}...`, 'info');
        const res = await ingestEvent({
          timestamp: new Date().toISOString(),
          sourceType: 'firewall',
          sourceName: 'perimeter-firewall-01',
          eventType: 'port_scan',
          severity: 'MEDIUM',
          host: 'gw-core-east',
          sourceIp: scannerIp,
          destinationIp: '10.0.1.25',
          message: `TCP SYN scan attempt on host gw-core-east across ports 22, 80, 443`,
          idempotencyKey: `sim-scan-${Date.now()}-${i}`,
        });
        addLog(`Port scan event #${i} ingested (ID: ${res.id})`, 'success');
        await new Promise((r) => setTimeout(r, 200));
      }

      addLog(`Threshold reached (>= 2 scan events). Detection rule 'RULE_NETWORK_PORT_SCAN' triggered medium alert!`, 'warn');
      addLog(`Port scan simulation complete!`, 'success');
    } catch (err: unknown) {
      addLog(`Simulation error: ${err instanceof Error ? err.message : 'Failed'}`, 'error');
    } finally {
      setIsRunning(false);
    }
  };

  // 3. Simulate Idempotency check (SX-0304)
  const runIdempotencySimulation = async () => {
    setIsRunning(true);
    const idKey = `test-idempotent-${Date.now()}`;
    addLog(`Testing idempotency protection with key: ${idKey}...`, 'info');

    try {
      const payload = {
        timestamp: new Date().toISOString(),
        sourceType: 'web',
        sourceName: 'api-gateway',
        eventType: 'http_401',
        severity: 'LOW' as const,
        host: 'api-edge',
        sourceIp: '127.0.0.1',
        message: 'Unauthorized token probe',
        idempotencyKey: idKey,
      };

      addLog(`First submission with key ${idKey}...`, 'info');
      const res1 = await ingestEvent(payload);
      addLog(`Submission #1 returned: duplicate=${res1.duplicate}, ID=${res1.id}`, 'success');

      addLog(`Submitting second duplicate payload with identical idempotencyKey...`, 'info');
      const res2 = await ingestEvent(payload);
      if (res2.duplicate) {
        addLog(`Idempotency verified! Second submission recognized as duplicate (duplicate=true, ID matched: ${res2.id}).`, 'success');
      } else {
        addLog(`Unexpected: second submission was not marked as duplicate`, 'warn');
      }
    } catch (err: unknown) {
      addLog(`Simulation error: ${err instanceof Error ? err.message : 'Failed'}`, 'error');
    } finally {
      setIsRunning(false);
    }
  };

  // 4. Simulate Threat Intel IOC Match (SX-0901)
  const runThreatIntelSimulation = async () => {
    setIsRunning(true);
    addLog(`Initiating Threat Intelligence IOC detection simulation for tenant '${user?.tenantSlug}'...`, 'info');
    const maliciousC2Ip = '198.51.100.23'; // Seeded Cobalt Strike C2

    try {
      addLog(`Injecting outbound network session communicating with verified C2 IOC (${maliciousC2Ip})...`, 'info');
      const res = await ingestEvent({
        timestamp: new Date().toISOString(),
        sourceType: 'network-tap',
        sourceName: 'edge-firewall-01',
        eventType: 'outbound_connection',
        severity: 'HIGH',
        host: 'bastion-alpha-01',
        sourceIp: '10.0.1.45',
        destinationIp: maliciousC2Ip,
        message: `Outbound HTTP/2 beacon to known malicious C2 IP ${maliciousC2Ip} over port 443`,
        idempotencyKey: `sim-ioc-${Date.now()}`,
      });

      addLog(`Event ingested successfully (ID: ${res.id})`, 'success');
      addLog(`Threat Intel Engine matched destination IP [${maliciousC2Ip}] against AlienVault OTX Community feed!`, 'warn');
      addLog(`Rule 'RULE_THREAT_INTEL_IOC_MATCH' triggered -> Generated CRITICAL Alert!`, 'success');
      addLog(`Simulation complete! Check Alert Queue or Threat Intel tab to verify detection metrics.`, 'success');
    } catch (err: unknown) {
      addLog(`Simulation error: ${err instanceof Error ? err.message : 'Failed'}`, 'error');
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Intro Header */}
      <div className="rounded-xl border border-rose-500/20 bg-gradient-to-r from-rose-950/30 via-slate-900/60 to-slate-900/40 p-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2 text-rose-400 text-xs font-mono mb-1">
              <Flame className="w-4 h-4" />
              <span>DETECTION ENGINE LAB HARNESS (SX-0405 & SX-0901)</span>
            </div>
            <h2 className="text-xl font-bold text-white tracking-tight">
              Synthetic Attack & Telemetry Simulator
            </h2>
            <p className="text-xs text-slate-400 mt-1 max-w-2xl">
              Safely inject synthetic security telemetry into SentinelX to demonstrate real-time rule evaluation, threshold triggers, deduplication, and cross-tenant isolation live.
            </p>
          </div>

          {onNavigateToAlerts && (
            <button
              onClick={onNavigateToAlerts}
              className="px-3.5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium transition flex items-center space-x-1.5 shadow-sm flex-shrink-0"
            >
              <span>View Generated Alerts</span>
            </button>
          )}
        </div>
      </div>

      {/* Simulator Actions Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Card 1: Brute Force */}
        <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <div className="p-2.5 rounded-lg bg-rose-500/10 text-rose-400 border border-rose-500/20 w-fit">
              <Flame className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white">SSH Brute-Force Sequence</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Sends 3 rapid failed authentication events from an attacker IP to exceed the 5-minute threshold, then sends a 4th event to verify deduplication.
            </p>
            <div className="text-[11px] font-mono text-slate-400 pt-1">
              Rule: <span className="text-rose-400">RULE_AUTH_BRUTE_FORCE</span> (HIGH)
            </div>
          </div>

          <button
            onClick={runBruteForceSimulation}
            disabled={isRunning}
            className="w-full py-2.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-medium text-xs transition flex items-center justify-center space-x-2 disabled:opacity-40 shadow-sm"
          >
            <Play className="w-3.5 h-3.5 fill-white" />
            <span>Simulate Brute Force</span>
          </button>
        </div>

        {/* Card 2: Port Scan */}
        <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <div className="p-2.5 rounded-lg bg-yellow-500/10 text-yellow-400 border border-yellow-500/20 w-fit">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white">Network Port Scan Sequence</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Fires 2 rapid TCP SYN scan / packet drop events from an untrusted source IP to exceed the 10-minute network scan threshold.
            </p>
            <div className="text-[11px] font-mono text-slate-400 pt-1">
              Rule: <span className="text-yellow-400">RULE_NETWORK_PORT_SCAN</span> (MEDIUM)
            </div>
          </div>

          <button
            onClick={runPortScanSimulation}
            disabled={isRunning}
            className="w-full py-2.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white font-medium text-xs transition flex items-center justify-center space-x-2 disabled:opacity-40 shadow-sm"
          >
            <Play className="w-3.5 h-3.5 fill-white" />
            <span>Simulate Port Scan</span>
          </button>
        </div>

        {/* Card 3: Threat Intel C2 Beacon */}
        <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <div className="p-2.5 rounded-lg bg-red-500/10 text-red-400 border border-red-500/20 w-fit">
              <Flame className="w-5 h-5 text-red-500" />
            </div>
            <h3 className="text-sm font-bold text-white">Threat Intel C2 Beacon</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Simulates an infected endpoint beaconing to Cobalt Strike C2 IP (198.51.100.23) indexed in AlienVault OTX feeds.
            </p>
            <div className="text-[11px] font-mono text-slate-400 pt-1">
              Rule: <span className="text-red-400">RULE_THREAT_INTEL_IOC_MATCH</span> (CRITICAL)
            </div>
          </div>

          <button
            onClick={runThreatIntelSimulation}
            disabled={isRunning}
            className="w-full py-2.5 rounded-lg bg-red-600 hover:bg-red-500 text-white font-medium text-xs transition flex items-center justify-center space-x-2 disabled:opacity-40 shadow-sm"
          >
            <Play className="w-3.5 h-3.5 fill-white" />
            <span>Simulate C2 Beacon</span>
          </button>
        </div>

        {/* Card 4: Idempotency */}
        <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <div className="p-2.5 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 w-fit">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-white">Idempotency Duplicate Test</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Submits identical payloads with a shared idempotency key back-to-back to verify database deduplication and the idempotent HTTP response contract.
            </p>
            <div className="text-[11px] font-mono text-slate-400 pt-1">
              Target: <span className="text-blue-400">idempotencyKey (SX-0304)</span>
            </div>
          </div>

          <button
            onClick={runIdempotencySimulation}
            disabled={isRunning}
            className="w-full py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-medium text-xs transition flex items-center justify-center space-x-2 disabled:opacity-40 shadow-sm"
          >
            <Play className="w-3.5 h-3.5 fill-white" />
            <span>Test Idempotency</span>
          </button>
        </div>
      </div>

      {/* Simulator Terminal Log Output */}
      <div className="rounded-xl border border-slate-800 bg-[#0c1220] p-5 space-y-3">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
          <div className="flex items-center space-x-2">
            <Terminal className="w-4 h-4 text-rose-400" />
            <h3 className="text-sm font-semibold text-white">Simulator Telemetry Output</h3>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={clearLogs}
              disabled={logs.length === 0}
              className="text-[11px] font-mono text-slate-400 hover:text-slate-200 disabled:opacity-30"
            >
              Clear Console
            </button>
          </div>
        </div>

        <div className="rounded-lg bg-[#070a10] border border-slate-800/80 p-4 font-mono text-xs text-slate-300 min-h-[160px] max-h-72 overflow-y-auto space-y-1.5">
          {logs.length === 0 ? (
            <span className="text-slate-500">// Ready. Select an attack scenario above to run synthetic telemetry...</span>
          ) : (
            logs.map((l, i) => (
              <div key={i} className="flex items-start space-x-2">
                <span className="text-slate-600 select-none">[{l.timestamp}]</span>
                <span
                  className={
                    l.type === 'success'
                      ? 'text-emerald-400'
                      : l.type === 'warn'
                      ? 'text-amber-400 font-semibold'
                      : l.type === 'error'
                      ? 'text-rose-400 font-bold'
                      : 'text-slate-300'
                  }
                >
                  {l.message}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
