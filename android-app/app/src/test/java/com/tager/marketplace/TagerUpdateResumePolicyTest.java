package com.tager.marketplace;

import static org.junit.Assert.assertEquals;

import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.junit.Test;

public class TagerUpdateResumePolicyTest {
    @Test
    public void downloadedUpdateAlwaysPromptsInstallFirst() {
        assertEquals(
                TagerUpdateResumePolicy.Action.PROMPT_INSTALL,
                TagerUpdateResumePolicy.decide(
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                        InstallStatus.DOWNLOADED,
                        true));
    }

    @Test
    public void activeFlexibleFlowIsResumed() {
        assertEquals(
                TagerUpdateResumePolicy.Action.RESUME_ACTIVE_FLOW,
                TagerUpdateResumePolicy.decide(
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                        InstallStatus.PENDING,
                        true));
    }

    @Test
    public void availableFlexibleUpdateGetsCooldownAfterReturn() {
        assertEquals(
                TagerUpdateResumePolicy.Action.COOLDOWN_AVAILABLE_UPDATE,
                TagerUpdateResumePolicy.decide(
                        UpdateAvailability.UPDATE_AVAILABLE,
                        InstallStatus.UNKNOWN,
                        true));
    }

    @Test
    public void unsupportedOrUnavailableUpdateClearsFlow() {
        assertEquals(
                TagerUpdateResumePolicy.Action.CLEAR_FLOW,
                TagerUpdateResumePolicy.decide(
                        UpdateAvailability.UPDATE_NOT_AVAILABLE,
                        InstallStatus.UNKNOWN,
                        true));
        assertEquals(
                TagerUpdateResumePolicy.Action.CLEAR_FLOW,
                TagerUpdateResumePolicy.decide(
                        UpdateAvailability.UPDATE_AVAILABLE,
                        InstallStatus.UNKNOWN,
                        false));
    }
}
