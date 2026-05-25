export interface HealthResponse {
  status: string;
  version: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
}

export interface Page<T> {
  items: T[];
  total: number;
  limit: number;
  offset: number;
}

export interface ProcessSummary {
  id: number;
  pid: number | null;
  name: string | null;
  path: string | null;
  signed: boolean | null;
  signer: string | null;
}

export interface EndpointSummary {
  id: number;
  ip: string;
  countryIso: string | null;
  countryName: string | null;
  asnOrg: string | null;
}

export interface ConnectionDto {
  id: number;
  protocol: string;
  state: string | null;
  localIp: string;
  localPort: number;
  remoteIp: string | null;
  remotePort: number | null;
  process: ProcessSummary | null;
  endpoint: EndpointSummary | null;
  latestScore: number | null;
  firstSeen: string;
  lastSeen: string;
  observationCount: number;
}

export interface ProcessDto {
  id: number;
  pid: number | null;
  name: string | null;
  path: string | null;
  signed: boolean | null;
  signer: string | null;
  connectionCount: number;
  maxScore: number | null;
  firstSeen: string;
  lastSeen: string;
}

export interface EndpointDto {
  id: number;
  ip: string;
  asn: number | null;
  asnOrg: string | null;
  countryIso: string | null;
  countryName: string | null;
  city: string | null;
  latitude: number | null;
  longitude: number | null;
  reverseDns: string | null;
  connectionCount: number;
  latestScore: number | null;
  firstSeen: string;
  lastSeen: string;
}

export type ObservationWindow = '1h' | '24h' | '7d' | '30d';

export interface ObservationBucket {
  bucketStart: string;
  bucketEnd: string;
  count: number;
}

export interface ThreatReason {
  rule?: string;
  points?: number;
  detail?: string;
  [key: string]: unknown;
}

export interface ThreatScoreDto {
  score: number;
  reasons: ThreatReason[];
  computedAt: string;
}

export interface EndpointScoresDto {
  latest: ThreatScoreDto | null;
  history: ThreatScoreDto[];
}

export interface RuleDto {
  code: string;
  displayName: string;
  description: string;
  defaultPoints: number;
  enabled: boolean;
}

export interface RuleUpdateRequest {
  enabled: boolean;
}

export interface TopProcessDto {
  id: number;
  pid: number | null;
  name: string | null;
  maxScore: number | null;
}

export interface SummaryDto {
  totalConnections: number;
  distinctProcesses: number;
  distinctEndpoints: number;
  meanScore: number | null;
  topProcesses: TopProcessDto[];
}

export interface ScoreTimelinePoint {
  minute: string;
  maxScore: number;
}

export interface ConnectionFilters {
  state?: string;
  minScore?: number;
  processName?: string;
  country?: string;
  limit?: number;
  offset?: number;
}
