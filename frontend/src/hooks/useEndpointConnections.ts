import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';
import type { ConnectionFilters } from '../lib/types';

export function useEndpointConnections(id: number | undefined, filters: ConnectionFilters = {}) {
  return useQuery({
    queryKey: ['endpoint-connections', id, filters],
    queryFn: () => apiClient.endpointConnections(id as number, filters),
    enabled: id !== undefined,
    staleTime: 10000
  });
}
