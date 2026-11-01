package com.nfu.pettravel.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @Test
    void shouldResolvePersistedCode() {
        assertThat(OrderStatus.fromCode(0)).isEqualTo(OrderStatus.WAIT_PAY);
        assertThat(OrderStatus.fromCode(1)).isEqualTo(OrderStatus.PAID);
        assertThat(OrderStatus.fromCode(2)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderStatus.fromCode(3)).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void shouldOnlyAllowLegalTransitions() {
        assertThat(OrderStatus.WAIT_PAY.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.WAIT_PAY.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.COMPLETED)).isTrue();

        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void shouldRejectUnknownPersistedCode() {
        assertThatThrownBy(() -> OrderStatus.fromCode(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }
}
