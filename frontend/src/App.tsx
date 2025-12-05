import React, { useState, useEffect } from 'react';
import { GameLobby } from './components/lobby/GameLobby';
import { GamePage } from './components/pages/GamePage';
import './App.css';

// Generate or retrieve player ID from localStorage
const getPlayerId = (): string => {
  const stored = localStorage.getItem('fore_player_id');
  if (stored) return stored;
  
  const newId = crypto.randomUUID();
  localStorage.setItem('fore_player_id', newId);
  return newId;
};

// Get or set player name
const getPlayerName = (): string => {
  return localStorage.getItem('fore_player_name') || '';
};

const setPlayerName = (name: string): void => {
  localStorage.setItem('fore_player_name', name);
};

type View = 'name-entry' | 'lobby' | 'game';

function App() {
  const [view, setView] = useState<View>('name-entry');
  const [playerId] = useState(getPlayerId);
  const [playerName, setName] = useState(getPlayerName);
  const [gameId, setGameId] = useState<string | null>(null);
  const [nameInput, setNameInput] = useState(playerName);

  // Check if we have a stored name
  useEffect(() => {
    if (playerName) {
      setView('lobby');
    }
  }, [playerName]);

  // Check URL for game ID (for direct links)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const urlGameId = params.get('game');
    if (urlGameId && playerName) {
      setGameId(urlGameId);
      setView('game');
    }
  }, [playerName]);

  const handleNameSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (nameInput.trim()) {
      const trimmedName = nameInput.trim();
      setName(trimmedName);
      setPlayerName(trimmedName);
      setView('lobby');
    }
  };

  const handleGameStart = (newGameId: string) => {
    setGameId(newGameId);
    setView('game');
    // Update URL for sharing
    window.history.pushState({}, '', `?game=${newGameId}`);
  };

  const handleLeaveGame = () => {
    setGameId(null);
    setView('lobby');
    window.history.pushState({}, '', '/');
  };

  // Name entry screen
  if (view === 'name-entry') {
    return (
      <div className="app name-entry">
        <div className="name-entry-card">
          <h1>🏌️ FORE</h1>
          <h2>Fairways & Greens</h2>
          <p>Enter your name to start playing</p>
          <form onSubmit={handleNameSubmit}>
            <input
              type="text"
              value={nameInput}
              onChange={(e) => setNameInput(e.target.value)}
              placeholder="Your name"
              maxLength={50}
              autoFocus
            />
            <button type="submit" disabled={!nameInput.trim()}>
              Start Playing
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Game lobby
  if (view === 'lobby') {
    return (
      <div className="app">
        <GameLobby
          playerId={playerId}
          playerName={playerName}
          onGameStart={handleGameStart}
        />
      </div>
    );
  }

  // Game view
  if (view === 'game' && gameId) {
    return (
      <div className="app">
        <GamePage
          gameId={gameId}
          playerId={playerId}
          playerName={playerName}
          onLeaveGame={handleLeaveGame}
        />
      </div>
    );
  }

  return null;
}

export default App;
