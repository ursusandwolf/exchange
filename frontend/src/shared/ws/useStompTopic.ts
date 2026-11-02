import { useEffect, useRef } from 'react';
import { getStompBroker } from './stompBroker';

export function useStompTopic<T>(topic: string, onMessage: (data: T) => void) {
  const handlerRef = useRef(onMessage);
  handlerRef.current = onMessage;

  useEffect(() => {
    const broker = getStompBroker();
    return broker.subscribe(topic, (body) => {
      try {
        handlerRef.current(JSON.parse(body) as T);
      } catch (error) {
        console.error('Failed to parse STOMP message', topic, error);
      }
    });
  }, [topic]);
}
