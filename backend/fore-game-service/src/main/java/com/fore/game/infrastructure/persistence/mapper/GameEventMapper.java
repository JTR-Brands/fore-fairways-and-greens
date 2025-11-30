package com.fore.game.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fore.game.domain.events.*;
import com.fore.game.infrastructure.persistence.entity.GameEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps between domain GameEvent and JPA GameEventEntity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventMapper {

    private final ObjectMapper objectMapper;

    public GameEventEntity toEntity(GameEvent event, long sequenceNum) {
        Map<String, Object> payload = objectMapper.convertValue(
                event,
                new TypeReference<Map<String, Object>>() {}
        );

        // Extract actor ID if present (player who caused the event)
        UUID actorId = extractActorId(event);

        return GameEventEntity.builder()
                .eventId(event.getEventId())
                .gameId(event.getGameId())
                .sequenceNum(sequenceNum)
                .eventType(event.getEventType())
                .actorId(actorId)
                .eventPayload(payload)
                .createdAt(event.getOccurredAt())
                .build();
    }

    /**
     * Convert entity back to domain event.
     * For the event log, we typically just need the raw data for replay/audit.
     * Full reconstruction is complex and usually not needed.
     */
    public Map<String, Object> toEventData(GameEventEntity entity) {
        Map<String, Object> data = new HashMap<>(entity.getEventPayload());
        data.put("eventId", entity.getEventId());
        data.put("eventType", entity.getEventType());
        data.put("gameId", entity.getGameId());
        data.put("sequenceNum", entity.getSequenceNum());
        data.put("occurredAt", entity.getCreatedAt());
        return data;
    }

    private UUID extractActorId(GameEvent event) {
        // Extract the player who caused this event, if applicable
        return switch (event) {
            case DiceRolledEvent e -> e.getPlayerId();
            case PlayerMovedEvent e -> e.getPlayerId();
            case PropertyPurchasedEvent e -> e.getPlayerId();
            case PropertyImprovedEvent e -> e.getPlayerId();
            case RentPaidEvent e -> e.getPayerId();
            case TurnEndedEvent e -> e.getPlayerId();
            case PlayerJoinedEvent e -> e.getPlayerId();
            case PlayerBankruptEvent e -> e.getPlayerId();
            case TradeProposedEvent e -> e.getOffer().getOfferingPlayerId();
            default -> null;
        };
    }
}
