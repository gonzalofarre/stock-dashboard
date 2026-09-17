package com.stockdashboard.strategy;

/** Extensible on purpose — RSI/Bollinger signals can join this enum later
 * without changing how StrategyService assembles or ranks results. */
public enum SignalType {
    EMA_CROSSOVER,
    MACD_CROSSOVER
}
