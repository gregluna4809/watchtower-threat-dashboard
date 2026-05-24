import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useSummary() {
  return useQuery({
    queryKey: ['summary'],
    queryFn: apiClient.summary,
    refetchInterval: 5000
  });
}
