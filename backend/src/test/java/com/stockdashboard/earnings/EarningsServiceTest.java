package com.stockdashboard.earnings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EarningsServiceTest {

    @Mock
    private EarningsClient earningsClient;

    private EarningsService earningsService;

    @BeforeEach
    void setUp() {
        earningsService = new EarningsService(earningsClient);
    }

    @Test
    void ordersUpcomingEarningsByReportDateAscending() {
        LocalDate today = LocalDate.now();
        when(earningsClient.getEarningsCalendar(any(), any(), any())).thenReturn(List.of(
                new EarningsEvent("LATE", today.plusDays(10), ReportTime.AFTER_CLOSE, BigDecimal.ONE, null),
                new EarningsEvent("SOON", today.plusDays(1), ReportTime.BEFORE_OPEN, BigDecimal.ONE, null),
                new EarningsEvent("MID", today.plusDays(5), ReportTime.UNSPECIFIED, BigDecimal.ONE, null)
        ));

        List<UpcomingEarningsResponse> result = earningsService.getUpcoming(5);

        assertThat(result).extracting(UpcomingEarningsResponse::ticker).containsExactly("SOON", "MID", "LATE");
    }

    @Test
    void excludesEventsBeforeToday() {
        LocalDate today = LocalDate.now();
        when(earningsClient.getEarningsCalendar(any(), any(), any())).thenReturn(List.of(
                new EarningsEvent("PAST", today.minusDays(1), ReportTime.AFTER_CLOSE, BigDecimal.ONE, null),
                new EarningsEvent("TODAY", today, ReportTime.BEFORE_OPEN, BigDecimal.ONE, null)
        ));

        List<UpcomingEarningsResponse> result = earningsService.getUpcoming(5);

        assertThat(result).extracting(UpcomingEarningsResponse::ticker).containsExactly("TODAY");
    }

    @Test
    void clampsUpcomingLimitToThe5To10Range() {
        LocalDate today = LocalDate.now();
        List<EarningsEvent> many = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> new EarningsEvent("T" + i, today.plusDays(i + 1), ReportTime.UNSPECIFIED, BigDecimal.ONE, null))
                .toList();
        when(earningsClient.getEarningsCalendar(any(), any(), any())).thenReturn(many);

        assertThat(earningsService.getUpcoming(1)).hasSize(5);
        assertThat(earningsService.getUpcoming(50)).hasSize(10);
        assertThat(earningsService.getUpcoming(7)).hasSize(7);
    }

    @Test
    void computesSurprisePercentAndOrdersDescending() {
        LocalDate reportDate = LocalDate.now().minusDays(3);
        when(earningsClient.getEarningsCalendar(any(), any(), any())).thenReturn(List.of(
                new EarningsEvent("BEAT_BIG", reportDate, ReportTime.AFTER_CLOSE, BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.20)),
                new EarningsEvent("MISS", reportDate, ReportTime.AFTER_CLOSE, BigDecimal.valueOf(2.00), BigDecimal.valueOf(1.80)),
                new EarningsEvent("BEAT_SMALL", reportDate, ReportTime.BEFORE_OPEN, BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.05))
        ));

        List<EarningsSurpriseResponse> result = earningsService.getBestSurprises(5);

        assertThat(result).extracting(EarningsSurpriseResponse::ticker).containsExactly("BEAT_BIG", "BEAT_SMALL", "MISS");
        assertThat(result.get(0).surprisePercent()).isEqualByComparingTo("20.00");
        assertThat(result.get(2).surprisePercent()).isEqualByComparingTo("-10.00");
    }

    @Test
    void excludesEventsWithoutAnActualOrAZeroEstimate() {
        LocalDate reportDate = LocalDate.now().minusDays(3);
        when(earningsClient.getEarningsCalendar(any(), any(), any())).thenReturn(List.of(
                new EarningsEvent("NOT_REPORTED", reportDate, ReportTime.AFTER_CLOSE, BigDecimal.ONE, null),
                new EarningsEvent("ZERO_ESTIMATE", reportDate, ReportTime.AFTER_CLOSE, BigDecimal.ZERO, BigDecimal.ONE),
                new EarningsEvent("VALID", reportDate, ReportTime.AFTER_CLOSE, BigDecimal.ONE, BigDecimal.valueOf(1.10))
        ));

        List<EarningsSurpriseResponse> result = earningsService.getBestSurprises(5);

        assertThat(result).extracting(EarningsSurpriseResponse::ticker).containsExactly("VALID");
    }
}
