import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';
import type { ConnectionFilters } from '../lib/types';

export function useConnections(filters: ConnectionFilters = {}) {
  return useQuery({
    queryKey: ['connections', filters],
    queryFn: () => apiClient.connections(filters),
    refetchInterval: 5000
  });
}
