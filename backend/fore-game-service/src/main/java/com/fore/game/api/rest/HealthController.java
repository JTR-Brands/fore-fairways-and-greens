package com.fore.game.api.rest;

import com.fore.game.application.ports.outbound.GameRepository;
import com.fore.game.domain.model.enums.GameStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final GameRepository gameRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "fore-game-service",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "gamesWaiting", gameRepository.countByStatus(GameStatus.WAITING),
                "gamesInProgress", gameRepository.countByStatus(GameStatus.IN_PROGRESS),
                "gamesCompleted", gameRepository.countByStatus(GameStatus.COMPLETED),
                "timestamp", Instant.now().toString()
        ));
    }
}
