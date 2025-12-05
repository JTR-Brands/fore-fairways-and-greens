import React from 'react';
import type { Board, PlayerState, Property, Tile as TileType } from '../../types/game';
import { getCourseGroupColor, getTileTypeIcon, formatCurrency } from '../../types/game';
import './GameBoard.css';

interface GameBoardProps {
  board: Board;
  players: PlayerState[];
  currentPlayerId: string | null;
  onTileClick?: (tile: TileType) => void;
  selectedPropertyId?: string | null;
}

export const GameBoard: React.FC<GameBoardProps> = ({
  board,
  players,
  currentPlayerId,
  onTileClick,
  selectedPropertyId,
}) => {
  // Board layout: 24 tiles in a square (7 per side, corners shared)
  // Top row: 0-6 (left to right)
  // Right column: 7-11 (top to bottom)
  // Bottom row: 12-18 (right to left)
  // Left column: 19-23 (bottom to top)

  const getPositionStyle = (position: number): React.CSSProperties => {
    const tileSize = 90;
    const boardSize = 7 * tileSize;

    if (position <= 6) {
      // Top row (left to right)
      return { top: 0, left: position * tileSize };
    } else if (position <= 11) {
      // Right column (top to bottom)
      return { top: (position - 6) * tileSize, left: boardSize - tileSize };
    } else if (position <= 18) {
      // Bottom row (right to left)
      return { top: boardSize - tileSize, left: (18 - position) * tileSize };
    } else {
      // Left column (bottom to top)
      return { top: (24 - position) * tileSize, left: 0 };
    }
  };

  const getPlayersOnTile = (position: number): PlayerState[] => {
    return players.filter((p) => p.position === position && !p.isBankrupt);
  };

  return (
    <div className="game-board">
      <div className="board-inner">
        {board.tiles.map((tile) => {
          const playersOnTile = getPlayersOnTile(tile.position);
          const isSelected = tile.property?.propertyId === selectedPropertyId;

          return (
            <div
              key={tile.position}
              className={`tile tile-${tile.type.toLowerCase()} ${isSelected ? 'selected' : ''}`}
              style={getPositionStyle(tile.position)}
              onClick={() => onTileClick?.(tile)}
            >
              <TileContent
                tile={tile}
                players={playersOnTile}
                currentPlayerId={currentPlayerId}
              />
            </div>
          );
        })}

        {/* Center area */}
        <div className="board-center">
          <h1 className="game-title">FORE</h1>
          <p className="game-subtitle">Fairways & Greens</p>
        </div>
      </div>
    </div>
  );
};

interface TileContentProps {
  tile: TileType;
  players: PlayerState[];
  currentPlayerId: string | null;
}

const TileContent: React.FC<TileContentProps> = ({ tile, players, currentPlayerId }) => {
  const isProperty = tile.type === 'PROPERTY' && tile.property;

  return (
    <div className="tile-content">
      {/* Property color bar */}
      {isProperty && tile.property && (
        <div
          className="property-color-bar"
          style={{ backgroundColor: getCourseGroupColor(tile.property.courseGroup) }}
        />
      )}

      {/* Tile icon for special tiles */}
      {!isProperty && (
        <div className="tile-icon">{getTileTypeIcon(tile.type)}</div>
      )}

      {/* Tile name */}
      <div className="tile-name">{tile.name}</div>

      {/* Property price */}
      {isProperty && tile.property && !tile.property.ownerId && (
        <div className="tile-price">
          {formatCurrency(tile.property.purchasePriceCents)}
        </div>
      )}

      {/* Ownership indicator */}
      {isProperty && tile.property?.ownerId && (
        <div className="ownership-indicator">
          {tile.property.improvementLevel !== 'NONE' && (
            <span className="improvement">
              {getImprovementIcon(tile.property.improvementLevel)}
            </span>
          )}
        </div>
      )}

      {/* Player tokens */}
      {players.length > 0 && (
        <div className="player-tokens">
          {players.map((player, idx) => (
            <PlayerToken
              key={player.playerId}
              player={player}
              isCurrentPlayer={player.playerId === currentPlayerId}
              offset={idx}
            />
          ))}
        </div>
      )}
    </div>
  );
};

interface PlayerTokenProps {
  player: PlayerState;
  isCurrentPlayer: boolean;
  offset: number;
}

const PlayerToken: React.FC<PlayerTokenProps> = ({ player, isCurrentPlayer, offset }) => {
  const tokenColors = ['#e74c3c', '#3498db', '#2ecc71', '#9b59b6'];
  const playerIndex = player.isNpc ? 1 : 0;

  return (
    <div
      className={`player-token ${isCurrentPlayer ? 'current' : ''} ${player.isNpc ? 'npc' : ''}`}
      style={{
        backgroundColor: tokenColors[playerIndex % tokenColors.length],
        transform: `translate(${offset * 8}px, ${offset * 8}px)`,
      }}
      title={player.displayName}
    >
      {player.isNpc ? '🤖' : '👤'}
    </div>
  );
};

const getImprovementIcon = (level: string): string => {
  switch (level) {
    case 'CLUBHOUSE_1':
      return '🏠';
    case 'CLUBHOUSE_2':
      return '🏠🏠';
    case 'CLUBHOUSE_3':
      return '🏠🏠🏠';
    case 'CLUBHOUSE_4':
      return '🏠🏠🏠🏠';
    case 'RESORT':
      return '🏨';
    default:
      return '';
  }
};

export default GameBoard;
