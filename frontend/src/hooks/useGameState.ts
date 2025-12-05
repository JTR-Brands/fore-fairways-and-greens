import { useState, useCallback, useEffect } from 'react';
import { gameApi } from '../services/gameApi';
import { useWebSocket } from './useWebSocket';
import type {
  GameStateResponse,
  GameUpdateNotification,
  DiceRoll,
  GameEvent,
  PlayerState,
  Property,
} from '../types/game';

interface UseGameStateOptions {
  gameId: string;
  playerId: string;
  playerName: string;
}

interface UseGameStateResult {
  // State
  gameState: GameStateResponse | null;
  isLoading: boolean;
  error: string | null;
  lastDiceRoll: DiceRoll | null;
  eventLog: GameEvent[];
  isConnected: boolean;

  // Current player helpers
  currentPlayer: PlayerState | null;
  opponent: PlayerState | null;
  isMyTurn: boolean;
  canRoll: boolean;
  canEndTurn: boolean;
  purchasableProperty: Property | null;

  // Actions
  rollDice: () => Promise<void>;
  purchaseProperty: (propertyId: string) => Promise<void>;
  improveProperty: (propertyId: string) => Promise<void>;
  endTurn: () => Promise<void>;
  refreshGameState: () => Promise<void>;
}

export function useGameState({
  gameId,
  playerId,
  playerName,
}: UseGameStateOptions): UseGameStateResult {
  const [gameState, setGameState] = useState<GameStateResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastDiceRoll, setLastDiceRoll] = useState<DiceRoll | null>(null);
  const [eventLog, setEventLog] = useState<GameEvent[]>([]);

  // Fetch full game state
  const refreshGameState = useCallback(async () => {
    try {
      const state = await gameApi.getGame(gameId);
      setGameState(state);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to fetch game state');
    }
  }, [gameId]);

  // Handle WebSocket notifications
  const handleUpdate = useCallback(
    (notification: GameUpdateNotification) => {
      console.log('Received update:', notification.updateType);

      // Update dice roll if present
      if (notification.diceRoll) {
        setLastDiceRoll(notification.diceRoll);
      }

      // Add events to log
      if (notification.events && notification.events.length > 0) {
        setEventLog((prev) => [...notification.events, ...prev].slice(0, 50));
      }

      // Fetch full state after notification
      refreshGameState();
    },
    [refreshGameState]
  );

  // WebSocket connection
  const { isConnected } = useWebSocket({
    gameId,
    playerId,
    playerName,
    onUpdate: handleUpdate,
    onConnect: () => {
      console.log('Connected to game WebSocket');
      refreshGameState();
    },
    onError: (err) => {
      console.error('WebSocket error:', err);
    },
  });

  // Initial load
  useEffect(() => {
    setIsLoading(true);
    refreshGameState().finally(() => setIsLoading(false));
  }, [refreshGameState]);

  // Computed values
  const currentPlayer = gameState?.players.find((p) => p.playerId === playerId) ?? null;
  const opponent = gameState?.players.find((p) => p.playerId !== playerId) ?? null;
  const isMyTurn = gameState?.currentPlayerId === playerId;
  const canRoll = isMyTurn && gameState?.turnPhase === 'ROLL';
  const canEndTurn = isMyTurn && gameState?.turnPhase === 'ACTION';

  // Find purchasable property at current position
  const purchasableProperty = (() => {
    if (!gameState || !currentPlayer || !isMyTurn || gameState.turnPhase !== 'ACTION') {
      return null;
    }
    const tile = gameState.board.tiles.find((t) => t.position === currentPlayer.position);
    if (tile?.property?.canBePurchased) {
      return tile.property;
    }
    return null;
  })();

  // Actions
  const rollDice = useCallback(async () => {
    if (!canRoll) return;
    setError(null);
    try {
      const result = await gameApi.rollDice(gameId, playerId);
      if (result.diceRoll) {
        setLastDiceRoll(result.diceRoll);
      }
      if (result.events) {
        setEventLog((prev) => [...result.events, ...prev].slice(0, 50));
      }
      setGameState(result.gameState);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to roll dice');
    }
  }, [gameId, playerId, canRoll]);

  const purchaseProperty = useCallback(
    async (propertyId: string) => {
      setError(null);
      try {
        const result = await gameApi.purchaseProperty(gameId, playerId, propertyId);
        if (result.events) {
          setEventLog((prev) => [...result.events, ...prev].slice(0, 50));
        }
        setGameState(result.gameState);
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to purchase property');
      }
    },
    [gameId, playerId]
  );

  const improveProperty = useCallback(
    async (propertyId: string) => {
      setError(null);
      try {
        const result = await gameApi.improveProperty(gameId, playerId, propertyId);
        if (result.events) {
          setEventLog((prev) => [...result.events, ...prev].slice(0, 50));
        }
        setGameState(result.gameState);
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Failed to improve property');
      }
    },
    [gameId, playerId]
  );

  const endTurn = useCallback(async () => {
    if (!canEndTurn) return;
    setError(null);
    try {
      const result = await gameApi.endTurn(gameId, playerId);
      if (result.events) {
        setEventLog((prev) => [...result.events, ...prev].slice(0, 50));
      }
      setGameState(result.gameState);
      setLastDiceRoll(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to end turn');
    }
  }, [gameId, playerId, canEndTurn]);

  return {
    gameState,
    isLoading,
    error,
    lastDiceRoll,
    eventLog,
    isConnected,
    currentPlayer,
    opponent,
    isMyTurn,
    canRoll,
    canEndTurn,
    purchasableProperty,
    rollDice,
    purchaseProperty,
    improveProperty,
    endTurn,
    refreshGameState,
  };
}
