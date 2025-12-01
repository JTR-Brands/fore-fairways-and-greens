package com.fore.game.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvailableGamesResponse {

    private List<GameSummary> games;
    private int totalCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameSummary {
        private UUID gameId;
        private String status;
        private String creatorName;
        private int playerCount;
        private int maxPlayers;
        private Instant createdAt;
    }
}
