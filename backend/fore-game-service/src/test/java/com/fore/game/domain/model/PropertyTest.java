package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.CourseGroup;
import com.fore.game.domain.model.enums.ImprovementLevel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PropertyTest {

    @Test
    void shouldCreateUnownedProperty() {
        // when
        Property property = createTestProperty();

        // then
        assertThat(property.getName()).isEqualTo("Test Property");
        assertThat(property.getCourseGroup()).isEqualTo(CourseGroup.LINKS_NINE);
        assertThat(property.isOwned()).isFalse();
        assertThat(property.getOwnerId()).isNull();
        assertThat(property.getImprovementLevel()).isEqualTo(ImprovementLevel.NONE);
        assertThat(property.isMortgaged()).isFalse();
    }

    @Test
    void shouldPurchaseProperty() {
        // given
        Property property = createTestProperty();
        UUID playerId = UUID.randomUUID();

        // when
        property.purchase(playerId);

        // then
        assertThat(property.isOwned()).isTrue();
        assertThat(property.isOwnedBy(playerId)).isTrue();
        assertThat(property.getOwnerId()).isEqualTo(playerId);
    }

    @Test
    void shouldNotAllowPurchasingAlreadyOwnedProperty() {
        // given
        Property property = createTestProperty();
        UUID playerId1 = UUID.randomUUID();
        property.purchase(playerId1);

        // when/then
        UUID playerId2 = UUID.randomUUID();
        assertThatThrownBy(() -> property.purchase(playerId2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already owned");
    }

    @Test
    void shouldTransferProperty() {
        // given
        Property property = createTestProperty();
        UUID playerId1 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        property.purchase(playerId1);

        // when
        property.transferTo(playerId2);

        // then
        assertThat(property.isOwnedBy(playerId2)).isTrue();
        assertThat(property.isOwnedBy(playerId1)).isFalse();
    }

    @Test
    void shouldCalculateBaseRentWithoutCompleteGroup() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());

        // when
        Money rent = property.calculateRent(false);

        // then
        assertThat(rent).isEqualTo(Money.ofDollars(50));
    }

    @Test
    void shouldDoubleRentWithCompleteGroupAndNoImprovements() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());

        // when
        Money rent = property.calculateRent(true);

        // then
        assertThat(rent).isEqualTo(Money.ofDollars(100)); // Base rent doubled
    }

    @Test
    void shouldCalculateRentWithClubhouse() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.improve();

        // when
        Money rentWithoutGroup = property.calculateRent(false);
        Money rentWithGroup = property.calculateRent(true);

        // then
        assertThat(rentWithoutGroup).isEqualTo(Money.ofDollars(200));
        // With improvement, complete group doesn't double the rent
        assertThat(rentWithGroup).isEqualTo(Money.ofDollars(200));
    }

    @Test
    void shouldCalculateRentWithResort() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.improve();
        property.improve();

        // when
        Money rent = property.calculateRent(true);

        // then
        assertThat(rent).isEqualTo(Money.ofDollars(500));
    }

    @Test
    void shouldCalculateZeroRentWhenMortgaged() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.mortgage();

        // when
        Money rent = property.calculateRent(true);

        // then
        assertThat(rent).isEqualTo(Money.zero());
    }

    @Test
    void shouldImproveProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());

        // when
        property.improve();

        // then
        assertThat(property.getImprovementLevel()).isEqualTo(ImprovementLevel.CLUBHOUSE);
    }

    @Test
    void shouldImproveToResort() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.improve();

        // when
        property.improve();

        // then
        assertThat(property.getImprovementLevel()).isEqualTo(ImprovementLevel.RESORT);
    }

    @Test
    void shouldNotAllowImprovingBeyondResort() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.improve();
        property.improve();

        // when/then
        assertThatThrownBy(() -> property.improve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum improvement");
    }

    @Test
    void shouldNotAllowImprovingMortgagedProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.mortgage();

        // when/then
        assertThatThrownBy(() -> property.improve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mortgaged property");
    }

    @Test
    void shouldMortgageProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());

        // when
        property.mortgage();

        // then
        assertThat(property.isMortgaged()).isTrue();
    }

    @Test
    void shouldNotAllowMortgagingAlreadyMortgagedProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.mortgage();

        // when/then
        assertThatThrownBy(() -> property.mortgage())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already mortgaged");
    }

    @Test
    void shouldNotAllowMortgagingPropertyWithImprovements() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.improve();

        // when/then
        assertThatThrownBy(() -> property.mortgage())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sell improvements before mortgaging");
    }

    @Test
    void shouldCalculateMortgageValue() {
        // given
        Property property = createTestProperty();

        // when
        Money mortgageValue = property.getMortgageValue();

        // then
        assertThat(mortgageValue).isEqualTo(Money.ofDollars(150)); // 50% of purchase price
    }

    @Test
    void shouldCalculateUnmortgageCost() {
        // given
        Property property = createTestProperty();

        // when
        Money unmortgageCost = property.getUnmortgageCost();

        // then
        assertThat(unmortgageCost).isEqualTo(Money.ofDollars(165)); // 110% of mortgage value
    }

    @Test
    void shouldUnmortgageProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.mortgage();

        // when
        property.unmortgage();

        // then
        assertThat(property.isMortgaged()).isFalse();
    }

    @Test
    void shouldNotAllowUnmortgagingNonMortgagedProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());

        // when/then
        assertThatThrownBy(() -> property.unmortgage())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not mortgaged");
    }

    @Test
    void shouldCheckIfPropertyCanBeImproved() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());

        // when/then
        assertThat(property.canBeImproved()).isTrue();

        property.improve();
        assertThat(property.canBeImproved()).isTrue();

        property.improve();
        assertThat(property.canBeImproved()).isFalse();
    }

    @Test
    void shouldNotBeAbleToImproveMortgagedProperty() {
        // given
        Property property = createTestProperty();
        property.purchase(UUID.randomUUID());
        property.mortgage();

        // when/then
        assertThat(property.canBeImproved()).isFalse();
    }

    private Property createTestProperty() {
        return Property.builder()
                .propertyId(UUID.randomUUID())
                .name("Test Property")
                .courseGroup(CourseGroup.LINKS_NINE)
                .tilePosition(1)
                .purchasePrice(Money.ofDollars(300))
                .baseRent(Money.ofDollars(50))
                .rentWithClubhouse(Money.ofDollars(200))
                .rentWithResort(Money.ofDollars(500))
                .improvementCost(Money.ofDollars(100))
                .build();
    }
}
