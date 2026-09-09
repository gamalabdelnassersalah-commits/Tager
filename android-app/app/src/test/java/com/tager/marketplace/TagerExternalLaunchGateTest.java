package com.tager.marketplace;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TagerExternalLaunchGateTest {
    @Test
    public void blocksImmediateDuplicateAndAllowsAfterWindow() {
        TagerExternalLaunchGate gate = new TagerExternalLaunchGate();

        assertTrue(gate.shouldAllow("external:whatsapp://send?text=hello", 1000L));
        assertFalse(gate.shouldAllow("external:whatsapp://send?text=hello", 1200L));
        assertFalse(gate.shouldAllow("external:whatsapp://send?text=hello", 1699L));
        assertTrue(gate.shouldAllow("external:whatsapp://send?text=hello", 1700L));
    }

    @Test
    public void differentTargetsAreNotBlocked() {
        TagerExternalLaunchGate gate = new TagerExternalLaunchGate();

        assertTrue(gate.shouldAllow("external:whatsapp://send?text=one", 5000L));
        assertTrue(gate.shouldAllow("external:geo:24.7136,46.6753", 5001L));
    }

    @Test
    public void emptyKeysAreRejectedAndClockRollbackDoesNotBlock() {
        TagerExternalLaunchGate gate = new TagerExternalLaunchGate();

        assertFalse(gate.shouldAllow(null, 1000L));
        assertFalse(gate.shouldAllow("", 1000L));
        assertTrue(gate.shouldAllow("external:market://details?id=com.tager.marketplace", 5000L));
        assertTrue(gate.shouldAllow("external:market://details?id=com.tager.marketplace", 4000L));
    }
}
