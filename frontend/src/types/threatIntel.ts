export type IocType = 'IP' | 'DOMAIN' | 'HASH_SHA256' | 'URL';

export type ThreatType =
  | 'MALWARE_C2'
  | 'RANSOMWARE'
  | 'PHISHING'
  | 'BOTNET'
  | 'SCANNER'
  | 'APT_INFRASTRUCTURE'
  | 'SUSPICIOUS';

export type ThreatSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface ThreatIntelIoc {
  id: string;
  tenantId: string | null;
  isGlobal: boolean;
  iocType: IocType;
  iocValue: string;
  threatType: ThreatType;
  severity: ThreatSeverity;
  confidence: number;
  sourceFeed: string;
  description: string;
  active: boolean;
  matchCount: number;
  lastMatchedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
}

export interface ThreatIntelStats {
  totalIocs: number;
  highCriticalIocs: number;
  totalMatches: number;
  activeFeedsCount: number;
  feedStatus: string;
}

export interface CreateIocPayload {
  iocType: IocType;
  iocValue: string;
  threatType: ThreatType;
  severity?: ThreatSeverity;
  confidence?: number;
  sourceFeed?: string;
  description?: string;
  isGlobal?: boolean;
}

export interface IocLookupResult {
  matched: boolean;
  queryValue: string;
  ioc: ThreatIntelIoc | null;
}
