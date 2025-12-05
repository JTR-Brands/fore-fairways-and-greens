import React, { useState, useEffect } from 'react';
import { gameApi } from '../../services/gameApi';
import type { Difficulty, GameSummary } from '../../types/game';
import './GameLobby.css';

interface GameLobbyProps {
  playerId: string;
  playerName: string;
  onGameStart: (gameId: string) => void;
}

export const GameLobby: React.FC<GameLobbyProps> = ({
  playerId,
  playerName,
  onGameStart,
}) => {
  const [availableGames, setAvailableGames] = useState<GameSummary[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedDifficulty, setSelectedDifficulty] = useState<Difficulty>('MEDIUM');
  const [isCreating, setIsCreating] = useState(false);

  // Fetch available games
  const fetchGames = async () => {
    try {
      const response = await gameApi.getAvailableGames();
      setAvailableGames(response.games);
    } catch (e) {
      console.error('Failed to fetch games:', e);
    }
  };

  useEffect(() => {
    fetchGames();
    const interval = setInterval(fetchGames, 5000);
    return () => clearInterval(interval);
  }, []);

  // Create new game vs NPC
  const handleCreateVsNpc = async () => {
    setIsCreating(true);
    setError(null);
    try {
      const game = await gameApi.createGame({
        playerId,
        playerName,
        vsNpc: true,
        npcDifficulty: selectedDifficulty,
      });
      onGameStart(game.gameId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create game');
    } finally {
      setIsCreating(false);
    }
  };

  // Create game for human opponent
  const handleCreateVsHuman = async () => {
    setIsCreating(true);
    setError(null);
    try {
      const game = await gameApi.createGame({
        playerId,
        playerName,
        vsNpc: false,
      });
      onGameStart(game.gameId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create game');
    } finally {
      setIsCreating(false);
    }
  };

  // Join existing game
  const handleJoinGame = async (gameId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      await gameApi.joinGame(gameId, { playerId, playerName });
      onGameStart(gameId);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to join game');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="game-lobby">
      <div className="lobby-header">
        <h1>🏌️ FORE: Fairways & Greens</h1>
        <p className="welcome-message">Welcome, {playerName}!</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Create Game Section */}
      <div className="lobby-section">
        <h2>Start New Game</h2>

        <div className="create-options">
          {/* VS NPC */}
          <div className="create-card">
            <h3>🤖 Play vs AI</h3>
            <p>Challenge the computer opponent</p>

            <div className="difficulty-selector">
              <label>Difficulty:</label>
              <select
                value={selectedDifficulty}
                onChange={(e) => setSelectedDifficulty(e.target.value as Difficulty)}
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
                <option value="RUTHLESS">Ruthless</option>
              </select>
            </div>

            <button
              className="create-button"
              onClick={handleCreateVsNpc}
              disabled={isCreating}
            >
              {isCreating ? 'Creating...' : 'Start Game'}
            </button>
          </div>

          {/* VS Human */}
          <div className="create-card">
            <h3>👥 Play vs Human</h3>
            <p>Create a game and wait for opponent</p>

            <button
              className="create-button secondary"
              onClick={handleCreateVsHuman}
              disabled={isCreating}
            >
              {isCreating ? 'Creating...' : 'Create Lobby'}
            </button>
          </div>
        </div>
      </div>

      {/* Available Games Section */}
      <div className="lobby-section">
        <h2>Join Game</h2>

        {availableGames.length === 0 ? (
          <p className="no-games">No games waiting for players</p>
        ) : (
          <div className="game-list">
            {availableGames.map((game) => (
              <div key={game.gameId} className="game-card">
                <div className="game-info">
                  <span className="host-name">{game.hostPlayerName}'s Game</span>
                  <span className="player-count">
                    {game.playerCount}/2 players
                  </span>
                </div>
                <button
                  className="join-button"
                  onClick={() => handleJoinGame(game.gameId)}
                  disabled={isLoading}
                >
                  Join
                </button>
              </div>
            ))}
          </div>
        )}

        <button className="refresh-button" onClick={fetchGames}>
          🔄 Refresh
        </button>
      </div>
    </div>
  );
};

export default GameLobby;
