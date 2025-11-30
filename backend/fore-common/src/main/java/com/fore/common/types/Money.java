package com.fore.common.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object representing monetary amounts.
 * Stored internally as cents (long) to avoid floating-point precision issues.
 */
@Getter
@EqualsAndHashCode
public final class Money implements Comparable<Money> {

    private static final int CENTS_PER_DOLLAR = 100;

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    @JsonCreator
    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public static Money ofDollars(long dollars) {
        return new Money(dollars * CENTS_PER_DOLLAR);
    }

    public static Money ofDollars(double dollars) {
        return new Money(Math.round(dollars * CENTS_PER_DOLLAR));
    }

    public static Money zero() {
        return new Money(0);
    }

    @JsonValue
    public long toCents() {
        return cents;
    }

    public BigDecimal toDollars() {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(CENTS_PER_DOLLAR), 2, RoundingMode.HALF_UP);
    }

    public Money add(Money other) {
        return new Money(this.cents + other.cents);
    }

    public Money subtract(Money other) {
        return new Money(this.cents - other.cents);
    }

    public Money multiply(int factor) {
        return new Money(this.cents * factor);
    }

    public Money multiply(double factor) {
        return new Money(Math.round(this.cents * factor));
    }

    public boolean isGreaterThan(Money other) {
        return this.cents > other.cents;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.cents >= other.cents;
    }

    public boolean isLessThan(Money other) {
        return this.cents < other.cents;
    }

    public boolean isNegative() {
        return this.cents < 0;
    }

    public boolean isZero() {
        return this.cents == 0;
    }

    public boolean isPositive() {
        return this.cents > 0;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(this.cents, other.cents);
    }

    @Override
    public String toString() {
        return String.format("$%,.2f", toDollars());
    }
}
