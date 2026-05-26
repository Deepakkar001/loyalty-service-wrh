package com.loyaltyos.analytics.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnalyticsProgrammeSqlTest {

    @Test
    void programmeJoin_collatesBothCoalesceBranches() {
        String join = AnalyticsProgrammeSql.programmeJoin("cbc", "td");
        assertTrue(join.contains("COLLATE utf8mb4_unicode_ci"));
        assertTrue(join.contains("COALESCE(cbc.programme_uid COLLATE"));
        assertTrue(join.contains("COALESCE(td.programme_uid COLLATE"));
        assertTrue(!join.contains("CONVERT("));
        assertTrue(!join.contains(":programmeUid"));
    }

    @Test
    void programmeScope_usesCastOnBindParameter() {
        String scope = AnalyticsProgrammeSql.programmeScope("td");
        assertTrue(scope.contains("COLLATE utf8mb4_unicode_ci"));
        assertTrue(scope.contains("CAST(:programmeUid AS CHAR(64) CHARACTER SET utf8mb4)"));
        assertTrue(!scope.contains("CONVERT("));
        assertTrue(!scope.contains(":programmeUid COLLATE"));
    }

    @Test
    void programmeColumnEqualsParam_coalescesNullAndUsesCastOnBind() {
        String eq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        assertTrue(eq.contains("COALESCE(pl.programme_uid"));
        assertTrue(eq.contains("CAST(:programmeUid AS CHAR(64) CHARACTER SET utf8mb4)"));
        assertTrue(!eq.contains("CONVERT("));
        assertTrue(!eq.contains(":programmeUid COLLATE"));
    }
}
