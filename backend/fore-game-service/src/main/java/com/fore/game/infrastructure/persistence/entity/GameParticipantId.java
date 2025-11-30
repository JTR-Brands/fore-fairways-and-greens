package com.fore.game.infrastructure.persistence.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameParticipantId implements Serializable {
    private UUID gameId;
    private UUID playerId;
}
