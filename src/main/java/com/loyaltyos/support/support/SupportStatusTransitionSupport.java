package com.loyaltyos.support.support;

import com.loyaltyos.support.enums.SupportCaseStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class SupportStatusTransitionSupport {

    private static final Map<SupportCaseStatus, Set<SupportCaseStatus>> ADMIN_FROM = buildAdmin();
    private static final Map<SupportCaseStatus, Set<SupportCaseStatus>> TENANT_FROM = buildTenant();

    private SupportStatusTransitionSupport() {}

    public static boolean canTransition(
        SupportCaseStatus from,
        SupportCaseStatus to,
        boolean platformAdmin
    ) {
        if (from == null || to == null || from == to) {
            return false;
        }
        Map<SupportCaseStatus, Set<SupportCaseStatus>> matrix =
            platformAdmin ? ADMIN_FROM : TENANT_FROM;
        return matrix.getOrDefault(from, EnumSet.noneOf(SupportCaseStatus.class)).contains(to);
    }

    public static Set<SupportCaseStatus> allowedTargets(SupportCaseStatus from, boolean platformAdmin) {
        if (from == null) {
            return EnumSet.noneOf(SupportCaseStatus.class);
        }
        return EnumSet.copyOf(
            (platformAdmin ? ADMIN_FROM : TENANT_FROM).getOrDefault(from, EnumSet.noneOf(SupportCaseStatus.class))
        );
    }

    private static Map<SupportCaseStatus, Set<SupportCaseStatus>> buildAdmin() {
        Map<SupportCaseStatus, Set<SupportCaseStatus>> m = new EnumMap<>(SupportCaseStatus.class);
        m.put(SupportCaseStatus.OPEN, EnumSet.of(SupportCaseStatus.IN_PROGRESS, SupportCaseStatus.CLOSED));
        m.put(
            SupportCaseStatus.IN_PROGRESS,
            EnumSet.of(SupportCaseStatus.OPEN, SupportCaseStatus.RESOLVED, SupportCaseStatus.CLOSED)
        );
        m.put(
            SupportCaseStatus.RESOLVED,
            EnumSet.of(SupportCaseStatus.IN_PROGRESS, SupportCaseStatus.OPEN, SupportCaseStatus.CLOSED)
        );
        m.put(SupportCaseStatus.CLOSED, EnumSet.of(SupportCaseStatus.OPEN, SupportCaseStatus.IN_PROGRESS));
        return m;
    }

    private static Map<SupportCaseStatus, Set<SupportCaseStatus>> buildTenant() {
        Map<SupportCaseStatus, Set<SupportCaseStatus>> m = new EnumMap<>(SupportCaseStatus.class);
        m.put(SupportCaseStatus.OPEN, EnumSet.of(SupportCaseStatus.CLOSED));
        m.put(
            SupportCaseStatus.IN_PROGRESS,
            EnumSet.of(SupportCaseStatus.RESOLVED, SupportCaseStatus.CLOSED)
        );
        m.put(SupportCaseStatus.RESOLVED, EnumSet.of(SupportCaseStatus.OPEN, SupportCaseStatus.CLOSED));
        m.put(SupportCaseStatus.CLOSED, EnumSet.of(SupportCaseStatus.OPEN));
        return m;
    }
}
