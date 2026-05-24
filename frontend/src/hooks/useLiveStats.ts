import { useEffect, useSyncExternalStore } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { addStatusListener, getWebSocketStatus, subscribe } from '../lib/ws';
import type { SummaryDto } from '../lib/types';

export function useLiveStats() {
  const queryClient = useQueryClient();

  useEffect(() => {
    return subscribe<SummaryDto>('/topic/stats', (summary) => {
      queryClient.setQueryData(['summary'], summary);
    });
  }, [queryClient]);
}

export function useWebSocketStatus() {
  return useSyncExternalStore(addStatusListener, getWebSocketStatus, getWebSocketStatus);
}
