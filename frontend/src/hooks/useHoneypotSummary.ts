import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useHoneypotSummary() {
  return useQuery({
    queryKey: ['honeypot-summary'],
    queryFn: apiClient.honeypotSummary,
    refetchInterval: 5000
  });
}
