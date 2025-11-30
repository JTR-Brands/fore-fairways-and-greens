package com.fore.game.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DiceRollTest {

    @Test
    void shouldCreateValidDiceRoll() {
        // when
        DiceRoll roll = DiceRoll.of(3, 4);

        // then
        assertThat(roll.getDie1()).isEqualTo(3);
        assertThat(roll.getDie2()).isEqualTo(4);
        assertThat(roll.getTotal()).isEqualTo(7);
        assertThat(roll.isDoubles()).isFalse();
    }

    @Test
    void shouldDetectDoubles() {
        // when
        DiceRoll roll = DiceRoll.of(5, 5);

        // then
        assertThat(roll.isDoubles()).isTrue();
        assertThat(roll.getTotal()).isEqualTo(10);
    }

    @Test
    void shouldCalculateCorrectTotal() {
        // when
        DiceRoll minRoll = DiceRoll.of(1, 1);
        DiceRoll maxRoll = DiceRoll.of(6, 6);
        DiceRoll mixedRoll = DiceRoll.of(2, 5);

        // then
        assertThat(minRoll.getTotal()).isEqualTo(2);
        assertThat(maxRoll.getTotal()).isEqualTo(12);
        assertThat(mixedRoll.getTotal()).isEqualTo(7);
    }

    @Test
    void shouldRejectInvalidDiceValues() {
        // when/then
        assertThatThrownBy(() -> DiceRoll.of(0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dice values must be between 1 and 6");

        assertThatThrownBy(() -> DiceRoll.of(3, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dice values must be between 1 and 6");

        assertThatThrownBy(() -> DiceRoll.of(-1, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dice values must be between 1 and 6");
    }

    @Test
    void shouldGenerateRandomRoll() {
        // when
        DiceRoll roll = DiceRoll.roll();

        // then
        assertThat(roll.getDie1()).isBetween(1, 6);
        assertThat(roll.getDie2()).isBetween(1, 6);
        assertThat(roll.getTotal()).isBetween(2, 12);
    }

    @Test
    void shouldHaveCorrectEquality() {
        // given
        DiceRoll roll1 = DiceRoll.of(3, 4);
        DiceRoll roll2 = DiceRoll.of(3, 4);
        DiceRoll roll3 = DiceRoll.of(4, 3);

        // then
        assertThat(roll1).isEqualTo(roll2);
        assertThat(roll1.hashCode()).isEqualTo(roll2.hashCode());
        assertThat(roll1).isNotEqualTo(roll3);
    }

    @Test
    void shouldHaveCorrectStringRepresentation() {
        // given
        DiceRoll normalRoll = DiceRoll.of(3, 4);
        DiceRoll doublesRoll = DiceRoll.of(5, 5);

        // when/then
        assertThat(normalRoll.toString()).contains("3", "4", "7");
        assertThat(doublesRoll.toString()).contains("5", "5", "10", "doubles");
    }
}
