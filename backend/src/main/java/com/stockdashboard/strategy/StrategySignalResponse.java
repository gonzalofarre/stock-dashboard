package com.stockdashboard.strategy;

public record StrategySignalResponse(String ticker, SignalType type, Direction direction, int minutesAgo) {
}
