package com.stockdashboard.earnings;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions/earnings")
@RequiredArgsConstructor
public class EarningsController {

    private final EarningsService earningsService;

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingEarningsResponse>> upcoming(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(earningsService.getUpcoming(limit));
    }

    @GetMapping("/surprises")
    public ResponseEntity<List<EarningsSurpriseResponse>> surprises(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(earningsService.getBestSurprises(limit));
    }
}
