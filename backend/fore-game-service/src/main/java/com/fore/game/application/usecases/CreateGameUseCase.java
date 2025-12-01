package com.fore.game.application.usecases;

import com.fore.game.application.dto.CreateGameRequest;
import com.fore.game.application.dto.GameStateResponse;
import com.fore.game.application.dto.GameStateDtoMapper;
import com.fore.game.application.ports.outbound.GameEventRepository;
import com.fore.game.application.ports.outbound.GameRepository;
import com.fore.game.domain.events.GameEvent;
import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.enums.Difficulty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateGameUseCase {

    private final GameRepository gameRepository;
    private final GameEventRepository eventRepository;
    private final GameStateDtoMapper dtoMapper;

    @Transactional
    public GameStateResponse execute(CreateGameRequest request) {
        log.info("Creating new game for player: {} (vsNpc: {})", 
                request.getPlayerId(), request.isVsNpc());

        Difficulty difficulty = request.getNpcDifficulty() != null 
                ? request.getNpcDifficulty() 
                : Difficulty.MEDIUM;

        GameSession game = GameSession.create(
                request.getPlayerId(),
                request.getPlayerName(),
                request.isVsNpc(),
                difficulty
        );

        // Drain and persist events
        List<GameEvent> events = game.drainEvents();
        
        // Save game state
        GameSession savedGame = gameRepository.save(game);
        
        // Persist events
        eventRepository.appendEvents(savedGame.getGameId(), events);

        log.info("Created game: {} with status: {}", 
                savedGame.getGameId(), savedGame.getStatus());

        return dtoMapper.toGameStateResponse(savedGame);
    }
}
