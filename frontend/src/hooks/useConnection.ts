import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useConnection(id: number | undefined) {
  return useQuery({
    queryKey: ['connection', id],
    queryFn: () => apiClient.connection(id as number),
    enabled: id !== undefined
  });
}
