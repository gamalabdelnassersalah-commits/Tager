package com.tager.marketplace;

import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * Pure decision policy for recovering a Play in-app update when Tager resumes.
 * Keeping this logic free of Android lifecycle state makes cancellation and
 * downloaded-update behavior deterministic and unit-testable.
 */
final class TagerUpdateResumePolicy {
    enum Action {
        RESUME_ACTIVE_FLOW,
        PROMPT_INSTALL,
        COOLDOWN_AVAILABLE_UPDATE,
        CLEAR_FLOW
    }

    private TagerUpdateResumePolicy() { }

    static Action decide(int updateAvailability, int installStatus, boolean flexibleAllowed) {
        if (installStatus == InstallStatus.DOWNLOADED) {
            return Action.PROMPT_INSTALL;
        }
        if (updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && flexibleAllowed) {
            return Action.RESUME_ACTIVE_FLOW;
        }
        if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE && flexibleAllowed) {
            return Action.COOLDOWN_AVAILABLE_UPDATE;
        }
        return Action.CLEAR_FLOW;
    }
}
