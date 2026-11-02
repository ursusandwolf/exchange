import { Client, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

type ReadyListener = () => void;

class StompBroker {
  private readonly client: Client;
  private readonly readyListeners = new Set<ReadyListener>();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws-exchange'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.readyListeners.forEach((listener) => listener());
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame.headers['message'], frame.body);
      },
    });
    this.client.activate();
  }

  subscribe(topic: string, onBody: (body: string) => void): () => void {
    let subscription: StompSubscription | undefined;

    const attach = () => {
      if (subscription) return;
      subscription = this.client.subscribe(topic, (message) => onBody(message.body));
    };

    if (this.client.connected) {
      attach();
    } else {
      this.readyListeners.add(attach);
    }

    return () => {
      this.readyListeners.delete(attach);
      subscription?.unsubscribe();
      subscription = undefined;
    };
  }
}

let broker: StompBroker | null = null;

export function getStompBroker(): StompBroker {
  if (!broker) {
    broker = new StompBroker();
  }
  return broker;
}
