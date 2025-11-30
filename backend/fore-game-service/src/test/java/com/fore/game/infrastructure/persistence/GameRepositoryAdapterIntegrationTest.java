package com.fore.game.infrastructure.persistence;

import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.PlayerState;
import com.fore.game.domain.model.Property;
import com.fore.game.domain.model.enums.Difficulty;
import com.fore.game.domain.model.enums.GameStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {
        "com.fore.game.infrastructure.persistence",
        "com.fore.game.infrastructure.config"
})
class GameRepositoryAdapterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private GameRepositoryAdapter gameRepository;

    @Test
    void shouldSaveAndRetrieveGameSession() {
        // given
        UUID creatorId = UUID.randomUUID();
        GameSession game = GameSession.create(creatorId, "Test Player", false, null);

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getGameId()).isEqualTo(saved.getGameId());
        assertThat(retrieved.get().getPlayers()).hasSize(1);
    }

    @Test
    void shouldPersistPlayerState() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        PlayerState player = game.getPlayer(player1);
        player.moveTo(10);

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        PlayerState retrievedPlayer = retrieved.get().getPlayer(player1);
        assertThat(retrievedPlayer.getPosition()).isEqualTo(10);
        assertThat(retrievedPlayer.getDisplayName()).isEqualTo("Player 1");
    }

    @Test
    void shouldPersistPropertyOwnership() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        Property property = game.getBoard().getAllProperties().get(0);
        property.purchase(playerId);
        game.getPlayer(playerId).addProperty(property.getPropertyId());

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        Property retrievedProperty = retrieved.get().getBoard().getProperty(property.getPropertyId());
        assertThat(retrievedProperty.isOwnedBy(playerId)).isTrue();
        assertThat(retrieved.get().getPlayer(playerId).ownsProperty(property.getPropertyId())).isTrue();
    }

    @Test
    void shouldUpdateExistingGameSession() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);
        GameSession saved = gameRepository.save(game);

        // Modify the game
        saved.getPlayer(playerId).moveTo(5);

        // when
        GameSession updated = gameRepository.save(saved);
        Optional<GameSession> retrieved = gameRepository.findById(updated.getGameId());

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPlayer(playerId).getPosition()).isEqualTo(5);
    }

    @Test
    void shouldFindGamesByStatus() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        GameSession waitingGame = GameSession.create(player1, "Player 1", false, null);
        GameSession inProgressGame = GameSession.create(player2, "Player 2", true, Difficulty.EASY);

        gameRepository.save(waitingGame);
        gameRepository.save(inProgressGame);

        // when
        List<GameSession> waitingGames = gameRepository.findByStatus(GameStatus.WAITING);
        List<GameSession> inProgressGames = gameRepository.findByStatus(GameStatus.IN_PROGRESS);

        // then
        assertThat(waitingGames).hasSize(1);
        assertThat(waitingGames.get(0).getGameId()).isEqualTo(waitingGame.getGameId());
        assertThat(inProgressGames).hasSize(1);
        assertThat(inProgressGames.get(0).getGameId()).isEqualTo(inProgressGame.getGameId());
    }

    @Test
    void shouldFindGamesByPlayerId() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        GameSession game1 = GameSession.create(player1, "Player 1", true, Difficulty.EASY);
        GameSession game2 = GameSession.create(player2, "Player 2", true, Difficulty.EASY);

        gameRepository.save(game1);
        gameRepository.save(game2);

        // when
        List<GameSession> player1Games = gameRepository.findByPlayerId(player1);
        List<GameSession> player2Games = gameRepository.findByPlayerId(player2);

        // then
        assertThat(player1Games).hasSize(1);
        assertThat(player1Games.get(0).getGameId()).isEqualTo(game1.getGameId());
        assertThat(player2Games).hasSize(1);
        assertThat(player2Games.get(0).getGameId()).isEqualTo(game2.getGameId());
    }

    @Test
    void shouldFindActiveGamesByPlayerId() {
        // given
        UUID player1 = UUID.randomUUID();

        GameSession waitingGame = GameSession.create(player1, "Player 1", false, null);
        GameSession inProgressGame = GameSession.create(UUID.randomUUID(), "Creator", false, null);
        inProgressGame.joinGame(player1, "Player 1");

        gameRepository.save(waitingGame);
        gameRepository.save(inProgressGame);

        // when
        List<GameSession> activeGames = gameRepository.findActiveGamesByPlayerId(player1);

        // then
        assertThat(activeGames).hasSize(2);
        assertThat(activeGames).extracting(GameSession::getGameId)
                .containsExactlyInAnyOrder(waitingGame.getGameId(), inProgressGame.getGameId());
    }

    @Test
    void shouldDeleteGameById() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);
        GameSession saved = gameRepository.save(game);

        // when
        gameRepository.deleteById(saved.getGameId());
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldCheckIfGameExists() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);
        GameSession saved = gameRepository.save(game);

        // when
        boolean exists = gameRepository.existsById(saved.getGameId());
        boolean notExists = gameRepository.existsById(UUID.randomUUID());

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCountGamesByStatus() {
        // given
        GameSession game1 = GameSession.create(UUID.randomUUID(), "Player 1", false, null);
        GameSession game2 = GameSession.create(UUID.randomUUID(), "Player 2", false, null);
        GameSession game3 = GameSession.create(UUID.randomUUID(), "Player 3", true, Difficulty.EASY);

        gameRepository.save(game1);
        gameRepository.save(game2);
        gameRepository.save(game3);

        // when
        long waitingCount = gameRepository.countByStatus(GameStatus.WAITING);
        long inProgressCount = gameRepository.countByStatus(GameStatus.IN_PROGRESS);

        // then
        assertThat(waitingCount).isEqualTo(2);
        assertThat(inProgressCount).isEqualTo(1);
    }

    @Test
    void shouldPersistNpcPlayer() {
        // given
        UUID humanPlayer = UUID.randomUUID();
        GameSession game = GameSession.create(humanPlayer, "Human", true, Difficulty.HARD);

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getNpcPlayer()).isPresent();
        assertThat(retrieved.get().getNpcPlayer().get().getNpcDifficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    void shouldPersistBankruptPlayer() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        game.getPlayer(player2).declareBankrupt();

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPlayer(player2).isBankrupt()).isTrue();
    }

    @Test
    void shouldPersistSandTrapState() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        PlayerState player = game.getPlayer(playerId);
        player.enterSandTrap();

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        PlayerState retrievedPlayer = retrieved.get().getPlayer(playerId);
        assertThat(retrievedPlayer.isInSandTrap()).isTrue();
        assertThat(retrievedPlayer.getTurnsInSandTrap()).isEqualTo(3);
    }

    @Test
    void shouldPersistMultiplePropertiesOwnership() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);

        PlayerState player = game.getPlayer(playerId);
        Property prop1 = game.getBoard().getAllProperties().get(0);
        Property prop2 = game.getBoard().getAllProperties().get(1);

        prop1.purchase(playerId);
        prop2.purchase(playerId);
        player.addProperty(prop1.getPropertyId());
        player.addProperty(prop2.getPropertyId());

        // when
        GameSession saved = gameRepository.save(game);
        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then
        assertThat(retrieved).isPresent();
        PlayerState retrievedPlayer = retrieved.get().getPlayer(playerId);
        assertThat(retrievedPlayer.getPropertyCount()).isEqualTo(2);
        assertThat(retrievedPlayer.ownsProperty(prop1.getPropertyId())).isTrue();
        assertThat(retrievedPlayer.ownsProperty(prop2.getPropertyId())).isTrue();
    }

    @Test
    void shouldHandleConcurrentUpdates() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Player", true, Difficulty.EASY);
        GameSession saved = gameRepository.save(game);

        // when - simulate concurrent updates
        Optional<GameSession> session1 = gameRepository.findById(saved.getGameId());
        Optional<GameSession> session2 = gameRepository.findById(saved.getGameId());

        assertThat(session1).isPresent();
        assertThat(session2).isPresent();

        session1.get().getPlayer(playerId).moveTo(5);
        session2.get().getPlayer(playerId).moveTo(10);

        gameRepository.save(session1.get());
        gameRepository.save(session2.get());

        Optional<GameSession> retrieved = gameRepository.findById(saved.getGameId());

        // then - last write wins
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPlayer(playerId).getPosition()).isEqualTo(10);
    }
}
