import { afterEach, describe, expect, it, vi } from 'vitest';

import { API_BASE_URL, apiClient } from './apiClient';

describe('apiClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('uses same-origin API paths', () => {
    expect(API_BASE_URL).toBe('/api/v1');
  });

  it('requests the health endpoint with JSON headers', async () => {
    const health = {
      status: 'UP',
      version: 'test'
    };

    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(health), {
        status: 200,
        headers: {
          'Content-Type': 'application/json'
        }
      })
    );

    await expect(apiClient.health()).resolves.toEqual(health);
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/health`, {
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      }
    });
  });
});
