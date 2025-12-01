package com.fore.game.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionResultResponse {

    private boolean success;
    private String actionType;
    private List<GameEventDto> events;
    private GameStateResponse gameState;
    private DiceRollDto diceRoll;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameEventDto {
        private String eventType;
        private String description;
        private Object details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiceRollDto {
        private int die1;
        private int die2;
        private int total;
        private boolean doubles;
    }
}
