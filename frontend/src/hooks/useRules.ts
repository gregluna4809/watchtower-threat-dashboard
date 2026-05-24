import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../lib/apiClient';

export function useRules() {
  return useQuery({
    queryKey: ['rules'],
    queryFn: apiClient.rules
  });
}

export function useToggleRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, enabled }: { code: string; enabled: boolean }) =>
      apiClient.toggleRule(code, { enabled }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rules'] });
      void queryClient.invalidateQueries({ queryKey: ['connections'] });
    }
  });
}
