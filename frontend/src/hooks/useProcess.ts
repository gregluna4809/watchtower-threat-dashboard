import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useProcess(id: number | undefined) {
  return useQuery({
    queryKey: ['process', id],
    queryFn: () => apiClient.process(id as number),
    enabled: id !== undefined
  });
}
