import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useEndpoint(id: number | undefined) {
  return useQuery({
    queryKey: ['endpoint', id],
    queryFn: () => apiClient.endpoint(id as number),
    enabled: id !== undefined
  });
}
