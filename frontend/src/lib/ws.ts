import { Client, type IMessage, type IStompSocket, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://127.0.0.1:8088/ws';
const INITIAL_RECONNECT_DELAY_MS = 1000;
const MAX_RECONNECT_DELAY_MS = 30000;

export type WebSocketStatus = 'connected' | 'disconnected';

type StatusListener = () => void;

let reconnectDelay = INITIAL_RECONNECT_DELAY_MS;
let status: WebSocketStatus = 'disconnected';
const statusListeners = new Set<StatusListener>();

const client = new Client({
  webSocketFactory: () => new SockJS(WS_URL) as IStompSocket,
  reconnectDelay: INITIAL_RECONNECT_DELAY_MS,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  onConnect: () => {
    reconnectDelay = INITIAL_RECONNECT_DELAY_MS;
    client.reconnectDelay = reconnectDelay;
    setStatus('connected');
  },
  onWebSocketClose: () => {
    setStatus('disconnected');
    reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY_MS);
    client.reconnectDelay = reconnectDelay;
  },
  onStompError: () => {
    setStatus('disconnected');
  }
});

function setStatus(next: WebSocketStatus) {
  if (status === next) {
    return;
  }
  status = next;
  statusListeners.forEach((listener) => listener());
}

function ensureConnected() {
  if (!client.active) {
    client.activate();
  }
}

export function subscribe<T>(topic: string, onMessage: (payload: T) => void) {
  ensureConnected();
  let subscription: StompSubscription | null = null;

  const subscribeWhenConnected = () => {
    if (client.connected && subscription === null) {
      subscription = client.subscribe(topic, (message: IMessage) => {
        onMessage(JSON.parse(message.body) as T);
      });
    }
  };

  const removeStatusListener = addStatusListener(subscribeWhenConnected);
  subscribeWhenConnected();

  return () => {
    removeStatusListener();
    if (subscription !== null) {
      subscription.unsubscribe();
    }
  };
}

export function getWebSocketStatus() {
  return status;
}

export function addStatusListener(listener: StatusListener) {
  statusListeners.add(listener);
  ensureConnected();
  return () => {
    statusListeners.delete(listener);
  };
}
