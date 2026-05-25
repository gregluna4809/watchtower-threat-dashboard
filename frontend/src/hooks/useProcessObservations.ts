import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';
import type { ObservationWindow } from '../lib/types';

export function useProcessObservations(id: number | undefined, window: ObservationWindow) {
  return useQuery({
    queryKey: ['process-observations', id, window],
    queryFn: () => apiClient.processObservations(id as number, window),
    enabled: id !== undefined,
    staleTime: 10000
  });
}
