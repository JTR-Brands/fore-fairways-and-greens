import React from 'react';
import type { PlayerState, Board } from '../../types/game';
import { formatCurrency, getCourseGroupColor } from '../../types/game';
import './PlayerInfo.css';

interface PlayerInfoProps {
  player: PlayerState;
  board: Board;
  isCurrentTurn: boolean;
  isYou?: boolean;
}

export const PlayerInfo: React.FC<PlayerInfoProps> = ({
  player,
  board,
  isCurrentTurn,
  isYou = false,
}) => {
  // Get owned properties with details
  const ownedProperties = board.tiles
    .filter((t) => t.property && player.ownedPropertyIds.includes(t.property.propertyId))
    .map((t) => t.property!);

  // Group properties by course group
  const groupedProperties = ownedProperties.reduce(
    (acc, prop) => {
      if (!acc[prop.courseGroup]) {
        acc[prop.courseGroup] = [];
      }
      acc[prop.courseGroup].push(prop);
      return acc;
    },
    {} as Record<string, typeof ownedProperties>
  );

  // Calculate total property value
  const totalPropertyValue = ownedProperties.reduce(
    (sum, p) => sum + p.purchasePriceCents,
    0
  );

  // Calculate net worth
  const netWorth = player.currencyCents + totalPropertyValue;

  return (
    <div className={`player-info ${isCurrentTurn ? 'current-turn' : ''} ${isYou ? 'is-you' : ''}`}>
      <div className="player-header">
        <div className="player-avatar">
          {player.isNpc ? '🤖' : '👤'}
        </div>
        <div className="player-name-section">
          <h3 className="player-name">
            {player.displayName}
            {isYou && <span className="you-badge">YOU</span>}
          </h3>
          {player.isNpc && (
            <span className="npc-badge">{player.npcDifficulty} AI</span>
          )}
        </div>
        {isCurrentTurn && <div className="turn-badge">Playing</div>}
      </div>

      <div className="player-stats">
        <div className="stat">
          <span className="stat-label">Cash</span>
          <span className="stat-value cash">{formatCurrency(player.currencyCents)}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Properties</span>
          <span className="stat-value">{ownedProperties.length}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Net Worth</span>
          <span className="stat-value">{formatCurrency(netWorth)}</span>
        </div>
      </div>

      {ownedProperties.length > 0 && (
        <div className="properties-section">
          <h4>Properties</h4>
          <div className="property-groups">
            {Object.entries(groupedProperties).map(([group, props]) => (
              <div key={group} className="property-group">
                <div
                  className="group-indicator"
                  style={{ backgroundColor: getCourseGroupColor(group as any) }}
                />
                <div className="group-properties">
                  {props.map((prop) => (
                    <div key={prop.propertyId} className="property-chip">
                      <span className="property-chip-name">{prop.name}</span>
                      {prop.improvementLevel !== 'NONE' && (
                        <span className="improvement-badge">
                          {getImprovementShort(prop.improvementLevel)}
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {player.isInSandTrap && (
        <div className="status-alert sand-trap">
          ⚠️ In Sand Trap
        </div>
      )}

      {player.isBankrupt && (
        <div className="status-alert bankrupt">
          💀 Bankrupt
        </div>
      )}

      {player.consecutiveDoubles > 0 && (
        <div className="doubles-streak">
          🎲 Doubles streak: {player.consecutiveDoubles}
        </div>
      )}
    </div>
  );
};

const getImprovementShort = (level: string): string => {
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

export default PlayerInfo;
