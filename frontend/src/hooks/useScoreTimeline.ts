import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useScoreTimeline(window = '1h') {
  return useQuery({
    queryKey: ['scoreTimeline', window],
    queryFn: () => apiClient.scoreTimeline(window),
    refetchInterval: 60000
  });
}
