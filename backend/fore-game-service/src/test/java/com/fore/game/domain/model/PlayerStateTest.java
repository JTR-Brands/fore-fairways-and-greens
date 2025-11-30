package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PlayerStateTest {

    @Test
    void shouldCreateHumanPlayerWithBuilder() {
        // given
        UUID playerId = UUID.randomUUID();
        Money startingCurrency = Money.ofDollars(1500);

        // when
        PlayerState player = PlayerState.builder()
                .playerId(playerId)
                .displayName("Alice")
                .npc(false)
                .startingCurrency(startingCurrency)
                .build();

        // then
        assertThat(player.getPlayerId()).isEqualTo(playerId);
        assertThat(player.getDisplayName()).isEqualTo("Alice");
        assertThat(player.isNpc()).isFalse();
        assertThat(player.isHuman()).isTrue();
        assertThat(player.getNpcDifficulty()).isNull();
        assertThat(player.getCurrency()).isEqualTo(startingCurrency);
        assertThat(player.getPosition()).isZero();
        assertThat(player.isBankrupt()).isFalse();
        assertThat(player.isInSandTrap()).isFalse();
        assertThat(player.getPropertyCount()).isZero();
    }

    @Test
    void shouldCreateNpcPlayerWithDifficulty() {
        // given
        UUID npcId = UUID.randomUUID();
        Money startingCurrency = Money.ofDollars(1500);

        // when
        PlayerState npc = PlayerState.builder()
                .playerId(npcId)
                .displayName("AI Player")
                .npc(true)
                .npcDifficulty(Difficulty.HARD)
                .startingCurrency(startingCurrency)
                .build();

        // then
        assertThat(npc.isNpc()).isTrue();
        assertThat(npc.isHuman()).isFalse();
        assertThat(npc.getNpcDifficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    void shouldDefaultToMediumDifficultyForNpcWithoutDifficulty() {
        // when
        PlayerState npc = PlayerState.builder()
                .playerId(UUID.randomUUID())
                .displayName("AI Player")
                .npc(true)
                .startingCurrency(Money.ofDollars(1500))
                .build();

        // then
        assertThat(npc.getNpcDifficulty()).isEqualTo(Difficulty.MEDIUM);
    }

    @Test
    void shouldMoveToNewPosition() {
        // given
        PlayerState player = createTestPlayer();

        // when
        player.moveTo(5);

        // then
        assertThat(player.getPosition()).isEqualTo(5);
    }

    @Test
    void shouldAddCurrency() {
        // given
        PlayerState player = createTestPlayer();
        Money initial = player.getCurrency();

        // when
        player.addCurrency(Money.ofDollars(200));

        // then
        assertThat(player.getCurrency()).isEqualTo(initial.add(Money.ofDollars(200)));
    }

    @Test
    void shouldSubtractCurrency() {
        // given
        PlayerState player = createTestPlayer();
        Money initial = player.getCurrency();

        // when
        player.subtractCurrency(Money.ofDollars(100));

        // then
        assertThat(player.getCurrency()).isEqualTo(initial.subtract(Money.ofDollars(100)));
    }

    @Test
    void shouldThrowExceptionWhenSubtractingTooMuchCurrency() {
        // given
        PlayerState player = PlayerState.builder()
                .playerId(UUID.randomUUID())
                .displayName("Test")
                .npc(false)
                .startingCurrency(Money.ofDollars(100))
                .build();

        // when/then
        assertThatThrownBy(() -> player.subtractCurrency(Money.ofDollars(200)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot have negative currency");
    }

    @Test
    void shouldCheckIfPlayerCanAfford() {
        // given
        PlayerState player = PlayerState.builder()
                .playerId(UUID.randomUUID())
                .displayName("Test")
                .npc(false)
                .startingCurrency(Money.ofDollars(500))
                .build();

        // when/then
        assertThat(player.canAfford(Money.ofDollars(500))).isTrue();
        assertThat(player.canAfford(Money.ofDollars(400))).isTrue();
        assertThat(player.canAfford(Money.ofDollars(600))).isFalse();
    }

    @Test
    void shouldManagePropertyOwnership() {
        // given
        PlayerState player = createTestPlayer();
        UUID propertyId1 = UUID.randomUUID();
        UUID propertyId2 = UUID.randomUUID();

        // when
        player.addProperty(propertyId1);
        player.addProperty(propertyId2);

        // then
        assertThat(player.getPropertyCount()).isEqualTo(2);
        assertThat(player.ownsProperty(propertyId1)).isTrue();
        assertThat(player.ownsProperty(propertyId2)).isTrue();
        assertThat(player.getOwnedPropertyIds()).containsExactlyInAnyOrder(propertyId1, propertyId2);
    }

    @Test
    void shouldRemoveProperty() {
        // given
        PlayerState player = createTestPlayer();
        UUID propertyId = UUID.randomUUID();
        player.addProperty(propertyId);

        // when
        player.removeProperty(propertyId);

        // then
        assertThat(player.ownsProperty(propertyId)).isFalse();
        assertThat(player.getPropertyCount()).isZero();
    }

    @Test
    void shouldReturnUnmodifiablePropertySet() {
        // given
        PlayerState player = createTestPlayer();
        player.addProperty(UUID.randomUUID());

        // when/then
        assertThatThrownBy(() -> player.getOwnedPropertyIds().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldDeclareBankruptcy() {
        // given
        PlayerState player = createTestPlayer();

        // when
        player.declareBankrupt();

        // then
        assertThat(player.isBankrupt()).isTrue();
    }

    @Test
    void shouldManageSandTrapState() {
        // given
        PlayerState player = createTestPlayer();

        // when
        player.enterSandTrap();

        // then
        assertThat(player.isInSandTrap()).isTrue();
        assertThat(player.getTurnsInSandTrap()).isEqualTo(3);
    }

    @Test
    void shouldDecrementSandTrapTurns() {
        // given
        PlayerState player = createTestPlayer();
        player.enterSandTrap();

        // when
        player.decrementSandTrapTurns();

        // then
        assertThat(player.getTurnsInSandTrap()).isEqualTo(2);
        assertThat(player.isInSandTrap()).isTrue();
    }

    @Test
    void shouldEscapeSandTrapWhenTurnsReachZero() {
        // given
        PlayerState player = createTestPlayer();
        player.enterSandTrap();

        // when
        player.decrementSandTrapTurns();
        player.decrementSandTrapTurns();
        player.decrementSandTrapTurns();

        // then
        assertThat(player.getTurnsInSandTrap()).isZero();
        assertThat(player.isInSandTrap()).isFalse();
    }

    @Test
    void shouldEscapeSandTrapImmediately() {
        // given
        PlayerState player = createTestPlayer();
        player.enterSandTrap();

        // when
        player.escapeSandTrap();

        // then
        assertThat(player.isInSandTrap()).isFalse();
        assertThat(player.getTurnsInSandTrap()).isZero();
    }

    @Test
    void shouldManageConsecutiveDoubles() {
        // given
        PlayerState player = createTestPlayer();

        // when
        player.incrementConsecutiveDoubles();
        player.incrementConsecutiveDoubles();

        // then
        assertThat(player.getConsecutiveDoubles()).isEqualTo(2);
        assertThat(player.hasRolledThreeDoubles()).isFalse();
    }

    @Test
    void shouldDetectThreeConsecutiveDoubles() {
        // given
        PlayerState player = createTestPlayer();

        // when
        player.incrementConsecutiveDoubles();
        player.incrementConsecutiveDoubles();
        player.incrementConsecutiveDoubles();

        // then
        assertThat(player.getConsecutiveDoubles()).isEqualTo(3);
        assertThat(player.hasRolledThreeDoubles()).isTrue();
    }

    @Test
    void shouldResetConsecutiveDoubles() {
        // given
        PlayerState player = createTestPlayer();
        player.incrementConsecutiveDoubles();
        player.incrementConsecutiveDoubles();

        // when
        player.resetConsecutiveDoubles();

        // then
        assertThat(player.getConsecutiveDoubles()).isZero();
        assertThat(player.hasRolledThreeDoubles()).isFalse();
    }

    @Test
    void shouldCalculateNetWorthWithoutProperties() {
        // given
        PlayerState player = createTestPlayer();
        Board board = BoardFactory.createStandardBoard();

        // when
        Money netWorth = player.calculateNetWorth(board);

        // then
        assertThat(netWorth).isEqualTo(player.getCurrency());
    }

    @Test
    void shouldCalculateNetWorthWithProperties() {
        // given
        PlayerState player = createTestPlayer();
        Board board = BoardFactory.createStandardBoard();
        Property property = board.getAllProperties().get(0);

        // Purchase property
        player.subtractCurrency(property.getPurchasePrice());
        property.purchase(player.getPlayerId());
        player.addProperty(property.getPropertyId());

        // when
        Money netWorth = player.calculateNetWorth(board);

        // then - net worth should be starting currency (property cost subtracted, then added back as asset)
        assertThat(netWorth).isEqualTo(Money.ofDollars(1500));
    }

    private PlayerState createTestPlayer() {
        return PlayerState.builder()
                .playerId(UUID.randomUUID())
                .displayName("Test Player")
                .npc(false)
                .startingCurrency(Money.ofDollars(1500))
                .build();
    }
}
