import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';
import type { ObservationWindow } from '../lib/types';

export function useConnectionObservations(id: number | undefined, window: ObservationWindow) {
  return useQuery({
    queryKey: ['connection-observations', id, window],
    queryFn: () => apiClient.connectionObservations(id as number, window),
    enabled: id !== undefined,
    staleTime: 10000
  });
}
