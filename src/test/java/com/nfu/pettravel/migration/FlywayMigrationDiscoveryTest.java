package com.nfu.pettravel.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationDiscoveryTest {

    @Test
    void shouldDiscoverAllAtomicProductionMigrationsInOrder() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:flyway_discovery;MODE=MySQL", "sa", "")
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load();

        assertThat(flyway.info().pending())
                .extracting(info -> info.getVersion().toString())
                .containsExactly(
                        "20260822.01", "20260822.02", "20260822.03", "20260822.04",
                        "20260822.05", "20260822.06", "20260822.07");
    }
}
