import type {
  GameStateResponse,
  ActionResultResponse,
  CreateGameRequest,
  JoinGameRequest,
  PlayerActionRequest,
  GameSummary,
  ActionType,
} from '../types/game';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

class GameApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code?: string
  ) {
    super(message);
    this.name = 'GameApiError';
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    throw new GameApiError(
      errorBody.message || `HTTP ${response.status}`,
      response.status,
      errorBody.code
    );
  }
  return response.json();
}

export const gameApi = {
  // Game Management
  async createGame(request: CreateGameRequest): Promise<GameStateResponse> {
    const response = await fetch(`${API_BASE_URL}/games`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    return handleResponse<GameStateResponse>(response);
  },

  async joinGame(gameId: string, request: JoinGameRequest): Promise<GameStateResponse> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    return handleResponse<GameStateResponse>(response);
  },

  async getGame(gameId: string): Promise<GameStateResponse> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}`);
    return handleResponse<GameStateResponse>(response);
  },

  async getAvailableGames(): Promise<{ games: GameSummary[]; totalCount: number }> {
    const response = await fetch(`${API_BASE_URL}/games/available`);
    return handleResponse(response);
  },

  async getPlayerGames(playerId: string): Promise<{ games: GameSummary[]; totalCount: number }> {
    const response = await fetch(`${API_BASE_URL}/games/player/${playerId}`);
    return handleResponse(response);
  },

  // Game Actions
  async executeAction(gameId: string, request: PlayerActionRequest): Promise<ActionResultResponse> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}/actions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    return handleResponse<ActionResultResponse>(response);
  },

  // Convenience action methods
  async rollDice(gameId: string, playerId: string): Promise<ActionResultResponse> {
    return this.executeAction(gameId, {
      playerId,
      actionType: 'ROLL_DICE',
    });
  },

  async purchaseProperty(
    gameId: string,
    playerId: string,
    propertyId: string
  ): Promise<ActionResultResponse> {
    return this.executeAction(gameId, {
      playerId,
      actionType: 'PURCHASE_PROPERTY',
      targetPropertyId: propertyId,
    });
  },

  async improveProperty(
    gameId: string,
    playerId: string,
    propertyId: string
  ): Promise<ActionResultResponse> {
    return this.executeAction(gameId, {
      playerId,
      actionType: 'IMPROVE_PROPERTY',
      targetPropertyId: propertyId,
    });
  },

  async endTurn(gameId: string, playerId: string): Promise<ActionResultResponse> {
    return this.executeAction(gameId, {
      playerId,
      actionType: 'END_TURN',
    });
  },

  async proposeTrade(
    gameId: string,
    playerId: string,
    receivingPlayerId: string,
    offeredPropertyIds: string[],
    offeredCurrencyCents: number,
    requestedPropertyIds: string[],
    requestedCurrencyCents: number
  ): Promise<ActionResultResponse> {
    return this.executeAction(gameId, {
      playerId,
      actionType: 'PROPOSE_TRADE',
      tradeOffer: {
        receivingPlayerId,
        offeredPropertyIds,
        offeredCurrencyCents,
        requestedPropertyIds,
        requestedCurrencyCents,
      },
    });
  },

  async respondToTrade(
    gameId: string,
    playerId: string,
    accept: boolean
  ): Promise<ActionResultResponse> {
    return this.executeAction(gameId, {
      playerId,
      actionType: accept ? 'ACCEPT_TRADE' : 'REJECT_TRADE',
    });
  },

  // Health check
  async healthCheck(): Promise<{ status: string; service: string }> {
    const response = await fetch(`${API_BASE_URL}/health`);
    return handleResponse(response);
  },
};

export { GameApiError };
