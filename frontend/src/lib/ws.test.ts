import { describe, expect, it } from 'vitest';

import { LIVE_ENDPOINT_PATH } from './ws';

describe('websocket client', () => {
  it('uses same-origin live update paths', () => {
    expect(LIVE_ENDPOINT_PATH).toBe('/ws');
  });
});
