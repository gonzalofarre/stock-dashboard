package com.stockdashboard.strategy;

import com.stockdashboard.suggestions.Market;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions/strategy")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;

    @GetMapping
    public ResponseEntity<List<StrategySignalResponse>> signals(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "US") Market market
    ) {
        return ResponseEntity.ok(strategyService.getSignals(limit, market));
    }
}
