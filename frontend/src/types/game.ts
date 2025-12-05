// Game state types matching backend DTOs

export interface GameStateResponse {
  gameId: string;
  status: GameStatus;
  currentPlayerId: string | null;
  turnPhase: TurnPhase;
  turnNumber: number;
  winnerId: string | null;
  players: PlayerState[];
  board: Board;
  pendingTrade: TradeOffer | null;
  createdAt: string;
  updatedAt: string;
}

export type GameStatus = 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type TurnPhase = 'ROLL' | 'ACTION' | 'TRADE' | 'END_TURN';

export interface PlayerState {
  playerId: string;
  displayName: string;
  currencyCents: number;
  position: number;
  ownedPropertyIds: string[];
  isNpc: boolean;
  npcDifficulty: Difficulty | null;
  isBankrupt: boolean;
  isInSandTrap: boolean;
  consecutiveDoubles: number;
}

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD' | 'RUTHLESS';

export interface Board {
  tiles: Tile[];
}

export interface Tile {
  position: number;
  type: TileType;
  name: string;
  property: Property | null;
}

export type TileType = 
  | 'CLUBHOUSE_HQ'
  | 'PROPERTY'
  | 'PRO_SHOP'
  | 'SAND_TRAP'
  | 'MEMBERS_LOUNGE'
  | 'WATER_HAZARD';

export interface Property {
  propertyId: string;
  name: string;
  courseGroup: CourseGroup;
  purchasePriceCents: number;
  baseRentCents: number;
  currentRentCents: number;
  improvementCostCents: number;
  ownerId: string | null;
  improvementLevel: ImprovementLevel;
  isMortgaged: boolean;
  canBePurchased: boolean;
  canBeImproved: boolean;
}

export type CourseGroup = 
  | 'LINKS_NINE'
  | 'DESERT_NINE'
  | 'MOUNTAIN_NINE'
  | 'COASTAL_NINE'
  | 'CHAMPIONSHIP_NINE'
  | 'MASTERS_NINE';

export type ImprovementLevel = 
  | 'NONE'
  | 'CLUBHOUSE_1'
  | 'CLUBHOUSE_2'
  | 'CLUBHOUSE_3'
  | 'CLUBHOUSE_4'
  | 'RESORT';

export interface TradeOffer {
  offerId: string;
  offeringPlayerId: string;
  receivingPlayerId: string;
  offeredPropertyIds: string[];
  offeredCurrencyCents: number;
  requestedPropertyIds: string[];
  requestedCurrencyCents: number;
  status: TradeStatus;
}

export type TradeStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

// Action types
export interface ActionResultResponse {
  success: boolean;
  actionType: string;
  events: GameEvent[];
  gameState: GameStateResponse;
  diceRoll: DiceRoll | null;
  message: string | null;
}

export interface DiceRoll {
  die1: number;
  die2: number;
  total: number;
  isDoubles: boolean;
}

export interface GameEvent {
  eventType: string;
  description: string;
  details: Record<string, unknown>;
}

// WebSocket notification (lightweight)
export interface GameUpdateNotification {
  gameId: string;
  updateType: UpdateType;
  triggeredByPlayerId: string;
  turnNumber: number;
  currentPlayerId: string;
  turnPhase: TurnPhase;
  gameStatus: GameStatus;
  diceRoll: DiceRoll | null;
  events: GameEvent[];
  timestamp: string;
}

export type UpdateType = 
  | 'GAME_CREATED'
  | 'PLAYER_JOINED'
  | 'GAME_STARTED'
  | 'DICE_ROLLED'
  | 'PLAYER_MOVED'
  | 'PROPERTY_PURCHASED'
  | 'PROPERTY_IMPROVED'
  | 'RENT_PAID'
  | 'TRADE_PROPOSED'
  | 'TRADE_ACCEPTED'
  | 'TRADE_REJECTED'
  | 'TURN_ENDED'
  | 'PLAYER_BANKRUPT'
  | 'GAME_ENDED'
  | 'PLAYER_CONNECTED'
  | 'PLAYER_DISCONNECTED';

// Request types
export interface CreateGameRequest {
  playerId: string;
  playerName: string;
  vsNpc: boolean;
  npcDifficulty?: Difficulty;
}

export interface JoinGameRequest {
  playerId: string;
  playerName: string;
}

export interface PlayerActionRequest {
  playerId: string;
  actionType: ActionType;
  targetPropertyId?: string;
  tradeOffer?: TradeOfferRequest;
}

export type ActionType = 
  | 'ROLL_DICE'
  | 'PURCHASE_PROPERTY'
  | 'IMPROVE_PROPERTY'
  | 'PROPOSE_TRADE'
  | 'ACCEPT_TRADE'
  | 'REJECT_TRADE'
  | 'END_TURN';

export interface TradeOfferRequest {
  receivingPlayerId: string;
  offeredPropertyIds: string[];
  offeredCurrencyCents: number;
  requestedPropertyIds: string[];
  requestedCurrencyCents: number;
}

// Game list
export interface GameSummary {
  gameId: string;
  status: GameStatus;
  playerCount: number;
  hostPlayerName: string;
  createdAt: string;
}

// Helper functions
export const formatCurrency = (cents: number): string => {
  return `$${(cents / 100).toLocaleString()}`;
};

export const getCourseGroupColor = (group: CourseGroup): string => {
  const colors: Record<CourseGroup, string> = {
    LINKS_NINE: '#8B4513',      // Brown
    DESERT_NINE: '#87CEEB',     // Light Blue
    MOUNTAIN_NINE: '#FF69B4',   // Pink
    COASTAL_NINE: '#FFA500',    // Orange
    CHAMPIONSHIP_NINE: '#FF0000', // Red
    MASTERS_NINE: '#FFD700',    // Gold
  };
  return colors[group];
};

export const getTileTypeIcon = (type: TileType): string => {
  const icons: Record<TileType, string> = {
    CLUBHOUSE_HQ: '🏠',
    PROPERTY: '⛳',
    PRO_SHOP: '🛒',
    SAND_TRAP: '⚠️',
    MEMBERS_LOUNGE: '🎰',
    WATER_HAZARD: '💧',
  };
  return icons[type];
};
