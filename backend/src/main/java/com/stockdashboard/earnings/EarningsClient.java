package com.stockdashboard.earnings;

import java.time.LocalDate;
import java.util.List;

public interface EarningsClient {

    /**
     * All known earnings events for the given tickers whose report date
     * falls within [from, to] (inclusive). Implementations may return events
     * for tickers not in the list — callers should still filter if that
     * matters — but must never return events outside the date range.
     */
    List<EarningsEvent> getEarningsCalendar(List<String> tickers, LocalDate from, LocalDate to);
}
