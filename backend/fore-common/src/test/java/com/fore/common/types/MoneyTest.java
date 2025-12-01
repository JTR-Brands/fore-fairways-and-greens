package com.fore.common.types;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    void ofDollars_shouldConvertToCents() {
        Money money = Money.ofDollars(15);

        assertThat(money.toCents()).isEqualTo(1500L);
    }

    @Test
    void ofCents_shouldPreserveCents() {
        Money money = Money.ofCents(1234L);

        assertThat(money.toCents()).isEqualTo(1234L);
    }

    @Test
    void toDollars_shouldConvertCorrectly() {
        Money money = Money.ofCents(1550L);

        assertThat(money.toDollars()).isEqualTo(new BigDecimal("15.50"));
    }

    @Test
    void add_shouldSumCorrectly() {
        Money a = Money.ofDollars(10);
        Money b = Money.ofDollars(5);

        Money result = a.add(b);

        assertThat(result.toCents()).isEqualTo(1500L);
    }

    @Test
    void subtract_shouldDiffCorrectly() {
        Money a = Money.ofDollars(10);
        Money b = Money.ofDollars(3);

        Money result = a.subtract(b);

        assertThat(result.toCents()).isEqualTo(700L);
    }

    @Test
    void multiply_shouldMultiplyCorrectly() {
        Money money = Money.ofDollars(10);

        assertThat(money.multiply(2).toCents()).isEqualTo(2000L);
        assertThat(money.multiply(1.5).toCents()).isEqualTo(1500L);
    }

    @Test
    void isGreaterThan_shouldCompareCorrectly() {
        Money a = Money.ofDollars(10);
        Money b = Money.ofDollars(5);

        assertThat(a.isGreaterThan(b)).isTrue();
        assertThat(b.isGreaterThan(a)).isFalse();
    }

    @Test
    void isNegative_shouldDetectNegative() {
        Money negative = Money.ofCents(-100L);
        Money positive = Money.ofCents(100L);
        Money zero = Money.zero();

        assertThat(negative.isNegative()).isTrue();
        assertThat(positive.isNegative()).isFalse();
        assertThat(zero.isNegative()).isFalse();
    }

    @Test
    void zero_shouldBeZero() {
        Money zero = Money.zero();

        assertThat(zero.toCents()).isEqualTo(0L);
        assertThat(zero.isZero()).isTrue();
    }

    @Test
    void toString_shouldFormatAsCurrency() {
        Money money = Money.ofCents(150099L);

        assertThat(money.toString()).isEqualTo("$1,500.99");
    }

    @Test
    void equality_shouldWorkCorrectly() {
        Money a = Money.ofDollars(10);
        Money b = Money.ofCents(1000L);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void compareTo_shouldOrderCorrectly() {
        Money small = Money.ofDollars(5);
        Money medium = Money.ofDollars(10);
        Money large = Money.ofDollars(15);

        assertThat(small.compareTo(medium)).isNegative();
        assertThat(medium.compareTo(medium)).isZero();
        assertThat(large.compareTo(medium)).isPositive();
    }
}
