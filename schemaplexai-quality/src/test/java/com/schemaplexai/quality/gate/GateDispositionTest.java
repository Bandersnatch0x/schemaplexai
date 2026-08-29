package com.schemaplexai.quality.gate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GateDispositionTest {

    @Test
    void mostSevereOrdersFailAboveBlockAboveWarnAbovePass() {
        assertThat(GateDisposition.mostSevere(GateDisposition.PASS, GateDisposition.WARN))
                .isEqualTo(GateDisposition.WARN);
        assertThat(GateDisposition.mostSevere(GateDisposition.WARN, GateDisposition.BLOCK))
                .isEqualTo(GateDisposition.BLOCK);
        assertThat(GateDisposition.mostSevere(GateDisposition.BLOCK, GateDisposition.FAIL))
                .isEqualTo(GateDisposition.FAIL);
        assertThat(GateDisposition.mostSevere(GateDisposition.FAIL, GateDisposition.PASS))
                .isEqualTo(GateDisposition.FAIL);
    }

    @Test
    void mostSevereTreatsNullAsPass() {
        assertThat(GateDisposition.mostSevere(null, GateDisposition.WARN)).isEqualTo(GateDisposition.WARN);
        assertThat(GateDisposition.mostSevere(GateDisposition.BLOCK, null)).isEqualTo(GateDisposition.BLOCK);
        assertThat(GateDisposition.mostSevere(null, null)).isEqualTo(GateDisposition.PASS);
    }

    @Test
    void severityFloorEscalatesOnlyCritical() {
        assertThat(GateDisposition.severityFloor("CRITICAL")).isEqualTo(GateDisposition.BLOCK);
        assertThat(GateDisposition.severityFloor("critical")).isEqualTo(GateDisposition.BLOCK);
        assertThat(GateDisposition.severityFloor("HIGH")).isEqualTo(GateDisposition.PASS);
        assertThat(GateDisposition.severityFloor("MEDIUM")).isEqualTo(GateDisposition.PASS);
        assertThat(GateDisposition.severityFloor("LOW")).isEqualTo(GateDisposition.PASS);
        assertThat(GateDisposition.severityFloor(null)).isEqualTo(GateDisposition.PASS);
    }

    @Test
    void parseOrDefaultIsLenientAndFailClosed() {
        assertThat(GateDisposition.parseOrDefault("warn", GateDisposition.BLOCK)).isEqualTo(GateDisposition.WARN);
        assertThat(GateDisposition.parseOrDefault(" FAIL ", GateDisposition.BLOCK)).isEqualTo(GateDisposition.FAIL);
        assertThat(GateDisposition.parseOrDefault("UNKNOWN", GateDisposition.BLOCK)).isEqualTo(GateDisposition.BLOCK);
        assertThat(GateDisposition.parseOrDefault(null, GateDisposition.WARN)).isEqualTo(GateDisposition.WARN);
        assertThat(GateDisposition.parseOrDefault("  ", GateDisposition.PASS)).isEqualTo(GateDisposition.PASS);
    }
}
