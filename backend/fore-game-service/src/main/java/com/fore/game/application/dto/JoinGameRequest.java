package com.fore.game.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinGameRequest {

    @NotNull(message = "Player ID is required")
    private UUID playerId;

    @NotBlank(message = "Player name is required")
    @Size(min = 1, max = 50, message = "Player name must be between 1 and 50 characters")
    private String playerName;
}
