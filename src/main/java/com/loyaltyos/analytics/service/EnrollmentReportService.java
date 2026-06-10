package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.EnrollmentReportResponse;
import com.loyaltyos.analytics.dto.EnrollmentSummary;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentReportService {

    private static final String ENROLLMENT_DEFINITION =
        "Enrollment is the customer's first CREDIT on the points ledger for the selected programme "
            + "(implicit join when a stable customerId first earns points).";

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public EnrollmentReportService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    public EnrollmentReportResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate ytdStart = LocalDate.of(to.getYear(), 1, 1);

        EnrollmentSummary summary = analyticsQueryRepository.getEnrollmentSummary(
            tenantId,
            programmeUid,
            from,
            to,
            priorFrom,
            ytdStart
        );

        EnrollmentReportResponse report = new EnrollmentReportResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setEnrollmentDefinition(ENROLLMENT_DEFINITION);
        report.setNewEnrollmentsInPeriod(summary.newEnrollmentsInPeriod());
        report.setNewEnrollmentsPriorPeriod(summary.newEnrollmentsPriorPeriod());
        report.setTotalEnrolledMembers(summary.totalEnrolledMembers());
        report.setReturningActiveInPeriod(summary.returningActiveInPeriod());
        report.setNewEnrollmentsYtd(summary.newEnrollmentsYtd());
        report.setPeriodOverPeriodChangePct(
            percentChange(summary.newEnrollmentsInPeriod(), summary.newEnrollmentsPriorPeriod())
        );
        report.setDailyNewEnrollments(
            analyticsQueryRepository.getDailyNewEnrollments(tenantId, programmeUid, from, to)
        );
        report.setMonthlyNewEnrollments(
            analyticsQueryRepository.getMonthlyNewEnrollments(tenantId, programmeUid, from, to)
        );
        report.setEnrollmentsBySource(
            analyticsQueryRepository.getEnrollmentsBySource(tenantId, programmeUid, from, to)
        );
        report.setTopEnrollmentRules(
            analyticsQueryRepository.getTopEnrollmentRules(tenantId, programmeUid, from, to, 10)
        );
        return report;
    }

    private static Double percentChange(long current, long prior) {
        if (prior == 0) {
            return current == 0 ? 0.0 : null;
        }
        return BigDecimal.valueOf(current - prior)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(prior), 1, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
