package com.loyaltyos.analytics.repository;

/**
 * Legacy rows may have {@code programme_uid IS NULL} (treated as {@code default}).
 * Comparisons collate every string operand to {@code utf8mb4_unicode_ci} so mixed table
 * defaults (0900 vs unicode) do not fail. Bind parameters use {@code CAST(...)} — never
 * {@code CONVERT(...)} (MySQL JDBC treats it as an escape and throws {@code Conversion = 'Y'}).
 */
final class AnalyticsProgrammeSql {

    private static final String COLLATE = "utf8mb4_unicode_ci";

    private AnalyticsProgrammeSql() {}

    private static String collate(String expression) {
        return expression + " COLLATE " + COLLATE;
    }

    private static String defaultProgrammeLiteral() {
        return "CAST('default' AS CHAR(64) CHARACTER SET utf8mb4) COLLATE " + COLLATE;
    }

    private static String programmeUidBind() {
        return "CAST(:programmeUid AS CHAR(64) CHARACTER SET utf8mb4) COLLATE " + COLLATE;
    }

    /** COALESCE branch with explicit collation so COALESCE does not inherit 0900 from the column. */
    private static String coalescedProgrammeColumn(String tableAlias) {
        return "COALESCE(" + collate(tableAlias + ".programme_uid") + ", " + defaultProgrammeLiteral() + ")";
    }

    static String tenantColumnEquals(String leftQualified, String rightQualified) {
        return collate(leftQualified) + " = " + collate(rightQualified);
    }

    static String programmeScope(String tableAlias) {
        return collate("COALESCE(" + collate(tableAlias + ".programme_uid") + ", " + defaultProgrammeLiteral() + ")")
            + " = "
            + programmeUidBind();
    }

    static String programmeJoin(String leftAlias, String rightAlias) {
        return coalescedProgrammeColumn(leftAlias) + " = " + coalescedProgrammeColumn(rightAlias);
    }

    static String programmeColumnEqualsParam(String qualifiedColumn) {
        return collate("COALESCE(" + collate(qualifiedColumn) + ", " + defaultProgrammeLiteral() + ")")
            + " = "
            + programmeUidBind();
    }

    static String programmeColumnEqualsColumn(String leftQualified, String rightQualified) {
        return collate(leftQualified) + " = " + collate(rightQualified);
    }

    /** Use in UNION branches when {@code customer_id} collations differ across tables. */
    static String collateColumn(String qualifiedColumn) {
        return collate(qualifiedColumn);
    }

    static String tierNameEqualsParam(String qualifiedColumn) {
        return collate(qualifiedColumn)
            + " = CAST(:tierName AS CHAR(128) CHARACTER SET utf8mb4) COLLATE "
            + COLLATE;
    }
}
