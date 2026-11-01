package com.nfu.pettravel.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnumTest {

    @Test
    void shouldKeepDatabaseCodesStable() {
        assertThat(UserRole.USER.getCode()).isZero();
        assertThat(UserRole.ADMIN.getCode()).isOne();
        assertThat(VaccineStatus.INCOMPLETE.getCode()).isZero();
        assertThat(VaccineStatus.COMPLETE.getCode()).isOne();
    }

    @Test
    void shouldIdentifyEligibleVaccineState() {
        assertThat(VaccineStatus.COMPLETE.isEligible()).isTrue();
        assertThat(VaccineStatus.INCOMPLETE.isEligible()).isFalse();
    }
}
