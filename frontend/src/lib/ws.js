import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Creates a STOMP client over SockJS for signaling, chat and notifications.
export function createStompClient(onConnect) {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 3000,
    onConnect,
  });
  client.activate();
  return client;
}
