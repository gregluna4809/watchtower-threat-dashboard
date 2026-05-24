import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useEndpoints() {
  return useQuery({
    queryKey: ['endpoints'],
    queryFn: apiClient.endpoints
  });
}
