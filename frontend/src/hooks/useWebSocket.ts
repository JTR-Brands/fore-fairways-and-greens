import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { GameUpdateNotification } from '../types/game';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

interface UseWebSocketOptions {
  gameId: string;
  playerId: string;
  playerName: string;
  onUpdate: (notification: GameUpdateNotification) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
}

interface UseWebSocketResult {
  isConnected: boolean;
  connect: () => void;
  disconnect: () => void;
}

export function useWebSocket({
  gameId,
  playerId,
  playerName,
  onUpdate,
  onConnect,
  onDisconnect,
  onError,
}: UseWebSocketOptions): UseWebSocketResult {
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);

  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        playerId,
        playerName,
      },
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[STOMP]', str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('WebSocket connected');
      setIsConnected(true);

      // Subscribe to game updates
      client.subscribe(
        `/topic/game/${gameId}`,
        (message: IMessage) => {
          try {
            const notification = JSON.parse(message.body) as GameUpdateNotification;
            onUpdate(notification);
          } catch (e) {
            console.error('Failed to parse WebSocket message:', e);
          }
        },
        {
          playerId,
          playerName,
        }
      );

      onConnect?.();
    };

    client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
      onDisconnect?.();
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message']);
      onError?.(new Error(frame.headers['message']));
    };

    client.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
      onError?.(new Error('WebSocket connection error'));
    };

    client.activate();
    clientRef.current = client;
  }, [gameId, playerId, playerName, onUpdate, onConnect, onDisconnect, onError]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (clientRef.current?.active) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    setIsConnected(false);
  }, []);

  // Auto-connect on mount
  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    connect,
    disconnect,
  };
}
