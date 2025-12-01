package com.fore.game.application.usecases;

import com.fore.game.application.dto.GameStateResponse;
import com.fore.game.application.dto.GameStateDtoMapper;
import com.fore.game.application.dto.JoinGameRequest;
import com.fore.game.application.ports.outbound.GameEventRepository;
import com.fore.game.application.ports.outbound.GameRepository;
import com.fore.game.domain.events.GameEvent;
import com.fore.game.domain.exceptions.GameNotFoundException;
import com.fore.game.domain.exceptions.InvalidGameStateException;
import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.enums.GameStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinGameUseCase {

    private final GameRepository gameRepository;
    private final GameEventRepository eventRepository;
    private final GameStateDtoMapper dtoMapper;

    @Transactional
    public GameStateResponse execute(UUID gameId, JoinGameRequest request) {
        log.info("Player {} joining game {}", request.getPlayerId(), gameId);

        GameSession game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new InvalidGameStateException("Game is not accepting players. Status: " + game.getStatus());
        }

        game.joinGame(request.getPlayerId(), request.getPlayerName());

        // Drain and persist events
        List<GameEvent> events = game.drainEvents();
        
        // Save game state
        GameSession savedGame = gameRepository.save(game);
        
        // Persist events
        eventRepository.appendEvents(savedGame.getGameId(), events);

        log.info("Player {} joined game {}. Game status: {}", 
                request.getPlayerId(), gameId, savedGame.getStatus());

        return dtoMapper.toGameStateResponse(savedGame);
    }
}
