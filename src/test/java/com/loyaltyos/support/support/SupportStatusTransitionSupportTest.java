package com.loyaltyos.support.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.loyaltyos.support.enums.SupportCaseStatus;
import org.junit.jupiter.api.Test;

class SupportStatusTransitionSupportTest {

    @Test
    void admin_canMoveOpenToInProgress() {
        assertThat(
            SupportStatusTransitionSupport.canTransition(
                SupportCaseStatus.OPEN,
                SupportCaseStatus.IN_PROGRESS,
                true
            )
        ).isTrue();
    }

    @Test
    void tenant_cannotMoveOpenToInProgress() {
        assertThat(
            SupportStatusTransitionSupport.canTransition(
                SupportCaseStatus.OPEN,
                SupportCaseStatus.IN_PROGRESS,
                false
            )
        ).isFalse();
    }

    @Test
    void tenant_canCloseOpenCase() {
        assertThat(
            SupportStatusTransitionSupport.canTransition(
                SupportCaseStatus.OPEN,
                SupportCaseStatus.CLOSED,
                false
            )
        ).isTrue();
    }
}
