package com.fore.game.application.ports.outbound;

import com.fore.game.domain.events.GameEvent;

import java.util.List;
import java.util.UUID;

/**
 * Port interface for game event persistence (append-only log).
 */
public interface GameEventRepository {

    void appendEvents(UUID gameId, List<GameEvent> events);

    List<GameEvent> findByGameId(UUID gameId);

    List<GameEvent> findByGameIdAfterSequence(UUID gameId, long afterSequenceNum);

    long getNextSequenceNumber(UUID gameId);
}
