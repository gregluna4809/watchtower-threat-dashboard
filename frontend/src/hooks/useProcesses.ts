import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useProcesses() {
  return useQuery({
    queryKey: ['processes'],
    queryFn: apiClient.processes
  });
}
