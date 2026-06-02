package com.loyaltyos.onboarding.exception;

import com.loyaltyos.onboarding.entity.Programme.ProgrammeStatus;

/**
 * Thrown when integration or runtime operations target a programme that is not {@link ProgrammeStatus#ACTIVE}.
 */
public class ProgrammeInactiveException extends RuntimeException {

    private final String programmeUid;
    private final ProgrammeStatus status;

    public ProgrammeInactiveException(String programmeUid, ProgrammeStatus status, String message) {
        super(message);
        this.programmeUid = programmeUid;
        this.status = status;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public ProgrammeStatus getStatus() {
        return status;
    }
}
