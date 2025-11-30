package com.fore.game.infrastructure.persistence.mapper;

import com.fore.game.domain.model.*;
import com.fore.game.domain.model.enums.Difficulty;
import com.fore.game.domain.model.enums.GameStatus;
import com.fore.game.domain.model.enums.ImprovementLevel;
import com.fore.game.domain.model.enums.TurnPhase;
import com.fore.game.infrastructure.persistence.entity.GameSessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class GameSessionMapperTest {

    private GameSessionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GameSessionMapper();
    }

    @Test
    void shouldMapDomainToEntity() {
        // given
        UUID creatorId = UUID.randomUUID();
        GameSession domain = GameSession.create(creatorId, "Test Player", false, null);

        // when
        GameSessionEntity entity = mapper.toEntity(domain);

        // then
        assertThat(entity).isNotNull();
        assertThat(entity.getGameId()).isEqualTo(domain.getGameId());
        assertThat(entity.getStatus()).isEqualTo(domain.getStatus().name());
        assertThat(entity.getTurnPhase()).isEqualTo(domain.getTurnPhase().name());
        assertThat(entity.getTurnNumber()).isEqualTo(domain.getTurnNumber());
        assertThat(entity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        assertThat(entity.getUpdatedAt()).isEqualTo(domain.getUpdatedAt());
        assertThat(entity.getParticipants()).hasSize(1);
        assertThat(entity.getGameStateSnapshot()).isNotNull();
    }

    @Test
    void shouldMapEntityToDomain() {
        // given
        UUID creatorId = UUID.randomUUID();
        GameSession originalDomain = GameSession.create(creatorId, "Test Player", false, null);
        GameSessionEntity entity = mapper.toEntity(originalDomain);

        // when
        GameSession reconstructedDomain = mapper.toDomain(entity);

        // then
        assertThat(reconstructedDomain).isNotNull();
        assertThat(reconstructedDomain.getGameId()).isEqualTo(originalDomain.getGameId());
        assertThat(reconstructedDomain.getStatus()).isEqualTo(originalDomain.getStatus());
        assertThat(reconstructedDomain.getTurnPhase()).isEqualTo(originalDomain.getTurnPhase());
        assertThat(reconstructedDomain.getTurnNumber()).isEqualTo(originalDomain.getTurnNumber());
        assertThat(reconstructedDomain.getPlayers()).hasSize(1);
    }

    @Test
    void shouldPreservePlayerStateInRoundTrip() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession originalGame = GameSession.create(player1, "Player 1", false, null);
        originalGame.joinGame(player2, "Player 2");

        PlayerState player = originalGame.getPlayer(player1);
        player.moveTo(5);

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        PlayerState reconstructedPlayer = reconstructed.getPlayer(player1);
        assertThat(reconstructedPlayer.getPosition()).isEqualTo(5);
        assertThat(reconstructedPlayer.getDisplayName()).isEqualTo("Player 1");
        assertThat(reconstructedPlayer.getCurrency()).isEqualTo(player.getCurrency());
    }

    @Test
    void shouldPreserveBoardStateInRoundTrip() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession originalGame = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        // Purchase a property
        Property property = originalGame.getBoard().getAllProperties().get(0);
        property.purchase(playerId);

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        Property reconstructedProperty = reconstructed.getBoard().getProperty(property.getPropertyId());
        assertThat(reconstructedProperty.isOwnedBy(playerId)).isTrue();
        assertThat(reconstructedProperty.getName()).isEqualTo(property.getName());
    }

    @Test
    void shouldPreservePropertyImprovementsInRoundTrip() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession originalGame = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        Board board = originalGame.getBoard();
        Property property = board.getAllProperties().get(0);
        property.purchase(playerId);
        property.improve();

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        Property reconstructedProperty = reconstructed.getBoard().getProperty(property.getPropertyId());
        assertThat(reconstructedProperty.getImprovementLevel()).isEqualTo(ImprovementLevel.CLUBHOUSE);
    }

    @Test
    void shouldPreserveNpcPlayerInRoundTrip() {
        // given
        UUID humanPlayer = UUID.randomUUID();
        GameSession originalGame = GameSession.create(humanPlayer, "Human", true, Difficulty.HARD);

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        assertThat(reconstructed.getNpcPlayer()).isPresent();
        assertThat(reconstructed.getNpcPlayer().get().getNpcDifficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    void shouldPreserveBankruptPlayerInRoundTrip() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession originalGame = GameSession.create(player1, "Player 1", false, null);
        originalGame.joinGame(player2, "Player 2");

        originalGame.getPlayer(player2).declareBankrupt();

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        assertThat(reconstructed.getPlayer(player2).isBankrupt()).isTrue();
    }

    @Test
    void shouldPreserveSandTrapStateInRoundTrip() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession originalGame = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        PlayerState player = originalGame.getPlayer(playerId);
        player.enterSandTrap();

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        PlayerState reconstructedPlayer = reconstructed.getPlayer(playerId);
        assertThat(reconstructedPlayer.isInSandTrap()).isTrue();
        assertThat(reconstructedPlayer.getTurnsInSandTrap()).isEqualTo(3);
    }

    @Test
    void shouldPreserveConsecutiveDoublesInRoundTrip() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession originalGame = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        PlayerState player = originalGame.getPlayer(playerId);
        player.incrementConsecutiveDoubles();
        player.incrementConsecutiveDoubles();

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        PlayerState reconstructedPlayer = reconstructed.getPlayer(playerId);
        assertThat(reconstructedPlayer.getConsecutiveDoubles()).isEqualTo(2);
    }

    @Test
    void shouldPreservePropertyOwnershipInRoundTrip() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession originalGame = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        Property property = originalGame.getBoard().getAllProperties().get(0);
        property.purchase(playerId);
        originalGame.getPlayer(playerId).addProperty(property.getPropertyId());

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        assertThat(reconstructed.getPlayer(playerId).ownsProperty(property.getPropertyId())).isTrue();
        assertThat(reconstructed.getBoard().getProperty(property.getPropertyId()).isOwnedBy(playerId)).isTrue();
    }

    @Test
    void shouldPreserveGameStatusInRoundTrip() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession originalGame = GameSession.create(player1, "Player 1", true, Difficulty.EASY);

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        assertThat(reconstructed.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void shouldPreserveTurnPhaseInRoundTrip() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession originalGame = GameSession.create(player1, "Player 1", true, Difficulty.EASY);

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        assertThat(reconstructed.getTurnPhase()).isEqualTo(TurnPhase.ROLL);
    }

    @Test
    void shouldPreserveCurrentPlayerIdInRoundTrip() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession originalGame = GameSession.create(player1, "Player 1", true, Difficulty.EASY);

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        assertThat(reconstructed.getCurrentPlayerId()).isEqualTo(player1);
    }

    @Test
    void shouldHandleMultiplePropertiesInRoundTrip() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession originalGame = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        PlayerState player = originalGame.getPlayer(playerId);
        Board board = originalGame.getBoard();

        // Purchase multiple properties
        Property prop1 = board.getAllProperties().get(0);
        Property prop2 = board.getAllProperties().get(1);
        Property prop3 = board.getAllProperties().get(2);

        prop1.purchase(playerId);
        prop2.purchase(playerId);
        prop3.purchase(playerId);

        player.addProperty(prop1.getPropertyId());
        player.addProperty(prop2.getPropertyId());
        player.addProperty(prop3.getPropertyId());

        // when
        GameSessionEntity entity = mapper.toEntity(originalGame);
        GameSession reconstructed = mapper.toDomain(entity);

        // then
        PlayerState reconstructedPlayer = reconstructed.getPlayer(playerId);
        assertThat(reconstructedPlayer.getPropertyCount()).isEqualTo(3);
        assertThat(reconstructedPlayer.ownsProperty(prop1.getPropertyId())).isTrue();
        assertThat(reconstructedPlayer.ownsProperty(prop2.getPropertyId())).isTrue();
        assertThat(reconstructedPlayer.ownsProperty(prop3.getPropertyId())).isTrue();
    }
}
