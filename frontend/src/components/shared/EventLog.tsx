import React from 'react';
import type { GameEvent } from '../../types/game';
import './EventLog.css';

interface EventLogProps {
  events: GameEvent[];
  maxEvents?: number;
}

export const EventLog: React.FC<EventLogProps> = ({ events, maxEvents = 20 }) => {
  const displayEvents = events.slice(0, maxEvents);

  const getEventIcon = (eventType: string): string => {
    const icons: Record<string, string> = {
      DICE_ROLLED: '🎲',
      PLAYER_MOVED: '🚶',
      PROPERTY_PURCHASED: '🏠',
      PROPERTY_IMPROVED: '🏗️',
      RENT_PAID: '💰',
      PASSED_GO: '🏁',
      LANDED_ON_SPECIAL: '⭐',
      TRADE_PROPOSED: '🤝',
      TRADE_ACCEPTED: '✅',
      TRADE_REJECTED: '❌',
      TURN_STARTED: '▶️',
      TURN_ENDED: '⏹️',
      GAME_STARTED: '🎮',
      GAME_ENDED: '🏆',
      PLAYER_BANKRUPT: '💀',
    };
    return icons[eventType] || '📌';
  };

  if (displayEvents.length === 0) {
    return (
      <div className="event-log empty">
        <p>No events yet</p>
      </div>
    );
  }

  return (
    <div className="event-log">
      <h3 className="event-log-title">Game Log</h3>
      <div className="event-list">
        {displayEvents.map((event, index) => (
          <div
            key={`${event.eventType}-${index}`}
            className={`event-item event-${event.eventType.toLowerCase()}`}
          >
            <span className="event-icon">{getEventIcon(event.eventType)}</span>
            <span className="event-description">{event.description}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default EventLog;
