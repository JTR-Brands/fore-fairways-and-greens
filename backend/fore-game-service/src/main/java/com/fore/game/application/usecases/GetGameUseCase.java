package com.fore.game.application.usecases;

import com.fore.game.application.dto.AvailableGamesResponse;
import com.fore.game.application.dto.GameStateResponse;
import com.fore.game.application.dto.GameStateDtoMapper;
import com.fore.game.application.ports.outbound.GameRepository;
import com.fore.game.domain.exceptions.GameNotFoundException;
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
public class GetGameUseCase {

    private final GameRepository gameRepository;
    private final GameStateDtoMapper dtoMapper;

    @Transactional(readOnly = true)
    public GameStateResponse getById(UUID gameId) {
        log.debug("Fetching game: {}", gameId);

        GameSession game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        return dtoMapper.toGameStateResponse(game);
    }

    @Transactional(readOnly = true)
    public AvailableGamesResponse getAvailableGames() {
        log.debug("Fetching available games");

        List<GameSession> waitingGames = gameRepository.findByStatus(GameStatus.WAITING);

        List<AvailableGamesResponse.GameSummary> summaries = waitingGames.stream()
                .map(dtoMapper::toGameSummary)
                .toList();

        return AvailableGamesResponse.builder()
                .games(summaries)
                .totalCount(summaries.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GameStateResponse> getGamesByPlayer(UUID playerId) {
        log.debug("Fetching games for player: {}", playerId);

        return gameRepository.findActiveGamesByPlayerId(playerId).stream()
                .map(dtoMapper::toGameStateResponse)
                .toList();
    }
}
