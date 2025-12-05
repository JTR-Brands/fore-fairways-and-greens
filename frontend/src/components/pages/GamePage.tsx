import React from 'react';
import { useGameState } from '../../hooks/useGameState';
import { GameBoard } from '../game/GameBoard';
import { DiceRoller } from '../game/DiceRoller';
import { ActionPanel } from '../game/ActionPanel';
import { PlayerInfo } from '../shared/PlayerInfo';
import { EventLog } from '../shared/EventLog';
import './GamePage.css';

interface GamePageProps {
  gameId: string;
  playerId: string;
  playerName: string;
  onLeaveGame: () => void;
}

export const GamePage: React.FC<GamePageProps> = ({
  gameId,
  playerId,
  playerName,
  onLeaveGame,
}) => {
  const {
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
  } = useGameState({ gameId, playerId, playerName });

  if (isLoading) {
    return (
      <div className="game-page loading">
        <div className="loading-spinner">Loading game...</div>
      </div>
    );
  }

  if (error && !gameState) {
    return (
      <div className="game-page error">
        <div className="error-message">
          <h2>Error</h2>
          <p>{error}</p>
          <button onClick={onLeaveGame}>Back to Lobby</button>
        </div>
      </div>
    );
  }

  if (!gameState) {
    return null;
  }

  // Find improvable properties
  const improvableProperties = gameState.board.tiles
    .filter((t) => t.property?.canBeImproved)
    .map((t) => t.property!);

  // Game over state
  if (gameState.status === 'COMPLETED') {
    const winner = gameState.players.find((p) => p.playerId === gameState.winnerId);
    const isWinner = gameState.winnerId === playerId;

    return (
      <div className="game-page game-over">
        <div className="game-over-modal">
          <h1>{isWinner ? '🏆 You Win!' : '😢 Game Over'}</h1>
          <p>{winner?.displayName} wins the game!</p>
          <button onClick={onLeaveGame}>Back to Lobby</button>
        </div>
      </div>
    );
  }

  return (
    <div className="game-page">
      {/* Header */}
      <header className="game-header">
        <div className="header-left">
          <button className="back-button" onClick={onLeaveGame}>
            ← Leave
          </button>
          <h1>FORE: Fairways & Greens</h1>
        </div>
        <div className="header-right">
          <span className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
            {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
          </span>
          <span className="turn-number">Turn {gameState.turnNumber}</span>
        </div>
      </header>

      {/* Error banner */}
      {error && <div className="error-banner">{error}</div>}

      {/* Main content */}
      <div className="game-content">
        {/* Left sidebar - Player info */}
        <aside className="sidebar left-sidebar">
          {currentPlayer && (
            <PlayerInfo
              player={currentPlayer}
              board={gameState.board}
              isCurrentTurn={isMyTurn}
              isYou={true}
            />
          )}
          {opponent && (
            <PlayerInfo
              player={opponent}
              board={gameState.board}
              isCurrentTurn={gameState.currentPlayerId === opponent.playerId}
            />
          )}
        </aside>

        {/* Center - Game board */}
        <main className="main-content">
          <GameBoard
            board={gameState.board}
            players={gameState.players}
            currentPlayerId={gameState.currentPlayerId}
          />
        </main>

        {/* Right sidebar - Actions and log */}
        <aside className="sidebar right-sidebar">
          <DiceRoller
            diceRoll={lastDiceRoll}
            canRoll={canRoll}
            onRoll={rollDice}
          />

          <ActionPanel
            gameState={gameState}
            currentPlayer={currentPlayer}
            isMyTurn={isMyTurn}
            canEndTurn={canEndTurn}
            purchasableProperty={purchasableProperty}
            improvableProperties={improvableProperties}
            onPurchase={purchaseProperty}
            onImprove={improveProperty}
            onEndTurn={endTurn}
          />

          <EventLog events={eventLog} />
        </aside>
      </div>
    </div>
  );
};

export default GamePage;
