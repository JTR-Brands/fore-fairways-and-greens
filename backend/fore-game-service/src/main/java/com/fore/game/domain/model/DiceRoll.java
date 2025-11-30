package com.fore.game.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Value object representing a dice roll (two six-sided dice).
 */
@Getter
@EqualsAndHashCode
public final class DiceRoll {

    private final int die1;
    private final int die2;

    public DiceRoll(int die1, int die2) {
        if (die1 < 1 || die1 > 6 || die2 < 1 || die2 > 6) {
            throw new IllegalArgumentException("Dice values must be between 1 and 6");
        }
        this.die1 = die1;
        this.die2 = die2;
    }

    public static DiceRoll roll() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new DiceRoll(random.nextInt(1, 7), random.nextInt(1, 7));
    }

    public static DiceRoll of(int die1, int die2) {
        return new DiceRoll(die1, die2);
    }

    public int getTotal() {
        return die1 + die2;
    }

    public boolean isDoubles() {
        return die1 == die2;
    }

    @Override
    public String toString() {
        return String.format("DiceRoll[%d, %d = %d%s]", 
            die1, die2, getTotal(), isDoubles() ? " (doubles)" : "");
    }
}
