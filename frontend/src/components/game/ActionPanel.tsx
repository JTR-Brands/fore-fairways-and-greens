import React from 'react';
import type { Property, PlayerState, GameStateResponse } from '../../types/game';
import { formatCurrency } from '../../types/game';
import './ActionPanel.css';

interface ActionPanelProps {
  gameState: GameStateResponse;
  currentPlayer: PlayerState | null;
  isMyTurn: boolean;
  canEndTurn: boolean;
  purchasableProperty: Property | null;
  improvableProperties: Property[];
  onPurchase: (propertyId: string) => void;
  onImprove: (propertyId: string) => void;
  onEndTurn: () => void;
}

export const ActionPanel: React.FC<ActionPanelProps> = ({
  gameState,
  currentPlayer,
  isMyTurn,
  canEndTurn,
  purchasableProperty,
  improvableProperties,
  onPurchase,
  onImprove,
  onEndTurn,
}) => {
  if (!currentPlayer) return null;

  const canAfford = (cents: number) => currentPlayer.currencyCents >= cents;

  return (
    <div className="action-panel">
      <div className="panel-header">
        <h3>Actions</h3>
        <span className={`turn-indicator ${isMyTurn ? 'my-turn' : ''}`}>
          {isMyTurn ? '🟢 Your Turn' : '🔴 Waiting...'}
        </span>
      </div>

      <div className="phase-indicator">
        Phase: <strong>{gameState.turnPhase}</strong>
      </div>

      {/* Purchase Property */}
      {purchasableProperty && isMyTurn && (
        <div className="action-card purchase">
          <h4>🏠 Purchase Property</h4>
          <p className="property-name">{purchasableProperty.name}</p>
          <p className="property-price">
            {formatCurrency(purchasableProperty.purchasePriceCents)}
          </p>
          <button
            className="action-button buy-button"
            onClick={() => onPurchase(purchasableProperty.propertyId)}
            disabled={!canAfford(purchasableProperty.purchasePriceCents)}
          >
            {canAfford(purchasableProperty.purchasePriceCents)
              ? 'Buy Property'
              : 'Cannot Afford'}
          </button>
        </div>
      )}

      {/* Improve Properties */}
      {improvableProperties.length > 0 && isMyTurn && canEndTurn && (
        <div className="action-card improve">
          <h4>🏗️ Improve Property</h4>
          <div className="improve-list">
            {improvableProperties.slice(0, 3).map((prop) => (
              <div key={prop.propertyId} className="improve-item">
                <span className="improve-name">{prop.name}</span>
                <span className="improve-cost">
                  {formatCurrency(prop.improvementCostCents)}
                </span>
                <button
                  className="action-button small"
                  onClick={() => onImprove(prop.propertyId)}
                  disabled={!canAfford(prop.improvementCostCents)}
                >
                  Build
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* End Turn */}
      {isMyTurn && canEndTurn && (
        <button className="action-button end-turn-button" onClick={onEndTurn}>
          End Turn ➡️
        </button>
      )}

      {/* Not your turn message */}
      {!isMyTurn && (
        <div className="waiting-message">
          <p>Waiting for opponent...</p>
          {gameState.currentPlayerId && (
            <p className="opponent-name">
              {gameState.players.find((p) => p.playerId === gameState.currentPlayerId)
                ?.displayName || 'Opponent'}{' '}
              is playing
            </p>
          )}
        </div>
      )}
    </div>
  );
};

export default ActionPanel;
