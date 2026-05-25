import type {
  ConnectionDto,
  ConnectionFilters,
  EndpointDto,
  EndpointScoresDto,
  ErrorResponse,
  HealthResponse,
  ObservationBucket,
  ObservationWindow,
  Page,
  ProcessDto,
  RuleDto,
  RuleUpdateRequest,
  ScoreTimelinePoint,
  SummaryDto
} from './types';

export const API_BASE_URL = 'http://127.0.0.1:8088/api/v1';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...init?.headers
    },
    ...init
  });

  if (!response.ok) {
    let error: ErrorResponse = {
      error: 'http_error',
      message: `Request failed with status ${response.status}`
    };

    try {
      error = (await response.json()) as ErrorResponse;
    } catch {
      // Keep the sanitized fallback above when the response body is not JSON.
    }

    throw new ApiError(response.status, error.error, error.message);
  }

  return (await response.json()) as T;
}

function withQuery(path: string, params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  });

  const query = search.toString();
  return query ? `${path}?${query}` : path;
}

export const apiClient = {
  health: () => request<HealthResponse>('/health'),
  connections: (filters: ConnectionFilters = {}) =>
    request<Page<ConnectionDto>>(
      withQuery('/connections', {
        limit: filters.limit ?? 100,
        offset: filters.offset ?? 0,
        state: filters.state,
        minScore: filters.minScore,
        processName: filters.processName,
        country: filters.country
      })
    ),
  connection: (id: number) => request<ConnectionDto>(`/connections/${id}`),
  connectionObservations: (id: number, window: ObservationWindow) =>
    request<ObservationBucket[]>(withQuery(`/connections/${id}/observations`, { window })),
  processes: () => request<Page<ProcessDto>>('/processes?limit=500&offset=0'),
  process: (id: number) => request<ProcessDto>(`/processes/${id}`),
  processConnections: (id: number, filters: ConnectionFilters = {}) =>
    request<Page<ConnectionDto>>(
      withQuery(`/processes/${id}/connections`, {
        limit: filters.limit ?? 100,
        offset: filters.offset ?? 0
      })
    ),
  processObservations: (id: number, window: ObservationWindow) =>
    request<ObservationBucket[]>(withQuery(`/processes/${id}/observations`, { window })),
  endpoints: () => request<Page<EndpointDto>>('/endpoints?limit=500&offset=0'),
  endpoint: (id: number) => request<EndpointDto>(`/endpoints/${id}`),
  endpointConnections: (id: number, filters: ConnectionFilters = {}) =>
    request<Page<ConnectionDto>>(
      withQuery(`/endpoints/${id}/connections`, {
        limit: filters.limit ?? 100,
        offset: filters.offset ?? 0
      })
    ),
  endpointObservations: (id: number, window: ObservationWindow) =>
    request<ObservationBucket[]>(withQuery(`/endpoints/${id}/observations`, { window })),
  endpointScores: (id: number, limit = 20) =>
    request<EndpointScoresDto>(withQuery(`/endpoints/${id}/scores`, { limit })),
  rules: () => request<RuleDto[]>('/rules'),
  toggleRule: (code: string, body: RuleUpdateRequest) =>
    request<RuleDto>(`/rules/${encodeURIComponent(code)}`, {
      method: 'PATCH',
      body: JSON.stringify(body)
    }),
  summary: () => request<SummaryDto>('/stats/summary'),
  scoreTimeline: (window: string) =>
    request<ScoreTimelinePoint[]>(withQuery('/stats/score-timeline', { window }))
};
