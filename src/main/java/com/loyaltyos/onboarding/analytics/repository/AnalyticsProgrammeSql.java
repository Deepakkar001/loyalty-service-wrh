package com.loyaltyos.onboarding.analytics.repository;

/**
 * Legacy tier rows may have {@code programme_uid IS NULL} while belonging to the {@code default} programme.
 */
final class AnalyticsProgrammeSql {

    private AnalyticsProgrammeSql() {}

    static String programmeScope(String tableAlias) {
        return "COALESCE(" + tableAlias + ".programme_uid, 'default') = :programmeUid";
    }

    static String programmeJoin(String leftAlias, String rightAlias) {
        return leftAlias + ".programme_uid = COALESCE(" + rightAlias + ".programme_uid, 'default')";
    }
}
