package com.rbctcsworld.ecommerce.qa.tests.ui;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** TEMPORARY - proves that a rerun-pass is reported as FLAKY in CI. Removed in the next commit. */
@Tag("ui")
class TemporaryFlakeProbeTest {
    @Test
    void failsOnceThenPasses() throws Exception {
        assumeTrue(System.getProperty("browser", "chrome").matches("firefox|edge"), "only in the cross-browser job (has the rerun)");
        Path marker = Path.of("target", "flake-probe.marker");
        boolean firstRun = !Files.exists(marker);
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "seen");
        assertTrue(!firstRun, "first attempt fails on purpose");
    }
}
