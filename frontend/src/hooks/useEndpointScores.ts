import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useEndpointScores(id: number | undefined, limit = 20) {
  return useQuery({
    queryKey: ['endpoint-scores', id, limit],
    queryFn: () => apiClient.endpointScores(id as number, limit),
    enabled: id !== undefined,
    staleTime: 10000
  });
}
