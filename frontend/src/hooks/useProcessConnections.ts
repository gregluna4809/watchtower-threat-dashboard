import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';
import type { ConnectionFilters } from '../lib/types';

export function useProcessConnections(id: number | undefined, filters: ConnectionFilters = {}) {
  return useQuery({
    queryKey: ['process-connections', id, filters],
    queryFn: () => apiClient.processConnections(id as number, filters),
    enabled: id !== undefined,
    staleTime: 10000
  });
}
