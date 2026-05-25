import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';
import type { ObservationWindow } from '../lib/types';

export function useEndpointObservations(id: number | undefined, window: ObservationWindow) {
  return useQuery({
    queryKey: ['endpoint-observations', id, window],
    queryFn: () => apiClient.endpointObservations(id as number, window),
    enabled: id !== undefined,
    staleTime: 10000
  });
}
