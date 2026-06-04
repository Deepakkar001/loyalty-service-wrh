package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.model.ReferralFraudPolicy;
import com.loyaltyos.referrals.model.ReferralLinkSignals;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.referrals.support.ReferralCodeGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReferralFraudService {

    private static final int DEFAULT_VELOCITY_WINDOW_HOURS = 24;

    private final ReferralRepository referralRepository;

    public ReferralFraudService(ReferralRepository referralRepository) {
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
    }

    public FraudCheckResult check(
        String tenantId,
        String programmeUid,
        String referrerCustomerId,
        String refereeCustomerId,
        ReferralProgrammeConfig config,
        ReferralLinkSignals referrerSignals,
        ReferralLinkSignals refereeSignals
    ) {
        ReferralFraudPolicy policy = config != null && config.getFraudPolicy() != null
            ? config.getFraudPolicy()
            : new ReferralFraudPolicy();

        FraudCheckResult result = new FraudCheckResult();
        result.setPassed(true);
        result.setReasons(new ArrayList<>());
        result.setCheckedAt(Instant.now());

        if (policy.isBlockSelfReferralByCustomerId() && referrerCustomerId.equals(refereeCustomerId)) {
            result.setPassed(false);
            result.getReasons().add("SELF_REFERRAL");
            return result;
        }

        if (referrerSignals != null && refereeSignals != null) {
            if (policy.isMatchPhoneWhenProvided()) {
                String rp = hashSignal(referrerSignals.getPhone());
                String rf = hashSignal(refereeSignals.getPhone());
                if (rp != null && rp.equals(rf)) {
                    result.setPassed(false);
                    result.getReasons().add("MATCHING_PHONE");
                }
            }
            if (policy.isMatchEmailWhenProvided()) {
                String re = hashSignal(referrerSignals.getEmail());
                String rfe = hashSignal(refereeSignals.getEmail());
                if (re != null && re.equals(rfe)) {
                    result.setPassed(false);
                    result.getReasons().add("MATCHING_EMAIL");
                }
            }
            if (policy.isMatchDeviceWhenProvided()) {
                String rd = hashSignal(referrerSignals.getDeviceId());
                String rfd = hashSignal(refereeSignals.getDeviceId());
                if (rd != null && rd.equals(rfd)) {
                    result.setPassed(false);
                    result.getReasons().add("MATCHING_DEVICE");
                }
            }
        }

        if (!result.isPassed()) {
            return result;
        }

        int maxPer24h = policy.getMaxReferralsPer24Hours() > 0
            ? policy.getMaxReferralsPer24Hours()
            : 20;
        Instant since = Instant.now().minus(DEFAULT_VELOCITY_WINDOW_HOURS, ChronoUnit.HOURS);
        long recent = referralRepository.findByTenantIdAndProgrammeUidAndReferrerCustomerId(
            tenantId, programmeUid, referrerCustomerId
        ).stream()
            .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(since))
            .count();

        if (recent >= maxPer24h) {
            result.setPassed(false);
            result.getReasons().add("SUSPICIOUS_VELOCITY");
        }

        return result;
    }

    private static String hashSignal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            return ReferralCodeGenerator.generate(16);
        }
    }

    public static class FraudCheckResult {
        private boolean passed;
        private List<String> reasons;
        private Instant checkedAt;

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public void setReasons(List<String> reasons) {
            this.reasons = reasons;
        }

        public Instant getCheckedAt() {
            return checkedAt;
        }

        public void setCheckedAt(Instant checkedAt) {
            this.checkedAt = checkedAt;
        }
    }
}
