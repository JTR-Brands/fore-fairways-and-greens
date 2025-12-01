package com.fore.game.domain;

import com.fore.game.domain.model.DiceRoll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DiceRollTest {

    @Test
    void of_shouldCreateWithValidValues() {
        DiceRoll roll = DiceRoll.of(3, 4);

        assertThat(roll.getDie1()).isEqualTo(3);
        assertThat(roll.getDie2()).isEqualTo(4);
        assertThat(roll.getTotal()).isEqualTo(7);
    }

    @Test
    void of_withInvalidValue_shouldThrow() {
        assertThatThrownBy(() -> DiceRoll.of(0, 4))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DiceRoll.of(3, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isDoubles_shouldDetectDoubles() {
        assertThat(DiceRoll.of(3, 3).isDoubles()).isTrue();
        assertThat(DiceRoll.of(6, 6).isDoubles()).isTrue();
        assertThat(DiceRoll.of(3, 4).isDoubles()).isFalse();
    }

    @RepeatedTest(100)
    void roll_shouldProduceValidValues() {
        DiceRoll roll = DiceRoll.roll();

        assertThat(roll.getDie1()).isBetween(1, 6);
        assertThat(roll.getDie2()).isBetween(1, 6);
        assertThat(roll.getTotal()).isBetween(2, 12);
    }

    @Test
    void getTotal_shouldSumDice() {
        assertThat(DiceRoll.of(1, 1).getTotal()).isEqualTo(2);
        assertThat(DiceRoll.of(6, 6).getTotal()).isEqualTo(12);
        assertThat(DiceRoll.of(3, 5).getTotal()).isEqualTo(8);
    }
}
