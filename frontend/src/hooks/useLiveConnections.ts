import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { subscribe } from '../lib/ws';
import type { ConnectionDto, Page } from '../lib/types';

export function useLiveConnections() {
  const queryClient = useQueryClient();

  useEffect(() => {
    return subscribe<ConnectionDto[]>('/topic/connections', (updates) => {
      queryClient.setQueriesData<Page<ConnectionDto>>({ queryKey: ['connections'] }, (oldPage) => {
        if (!oldPage) {
          return oldPage;
        }

        const byId = new Map(oldPage.items.map((item) => [item.id, item]));
        updates.forEach((update) => byId.set(update.id, update));
        const items = Array.from(byId.values())
          .sort((left, right) => new Date(right.lastSeen).getTime() - new Date(left.lastSeen).getTime())
          .slice(0, oldPage.limit);

        return {
          ...oldPage,
          items,
          total: Math.max(oldPage.total, byId.size)
        };
      });
    });
  }, [queryClient]);
}
