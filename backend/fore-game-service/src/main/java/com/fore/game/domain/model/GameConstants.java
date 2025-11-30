package com.fore.game.domain.model;

import com.fore.common.types.Money;

/**
 * Game configuration constants.
 */
public final class GameConstants {

    private GameConstants() {} // Prevent instantiation

    // Starting resources
    public static final Money STARTING_CURRENCY = Money.ofDollars(1500);

    // Salary for passing start
    public static final Money PASSING_SALARY = Money.ofDollars(200);

    // Penalties
    public static final Money WATER_HAZARD_PENALTY = Money.ofDollars(50);
    public static final Money SAND_TRAP_ESCAPE_COST = Money.ofDollars(50);

    // Board configuration
    public static final int TOTAL_TILES = 24;
    public static final int START_POSITION = 0;

    // Game rules
    public static final int MAX_TURNS_IN_SAND_TRAP = 3;
    public static final int DOUBLES_FOR_SAND_TRAP = 3;

    // Win conditions
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 2; // MVP: 2 players only
}
