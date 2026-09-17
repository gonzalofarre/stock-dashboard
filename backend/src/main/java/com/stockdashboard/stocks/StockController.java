package com.stockdashboard.stocks;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/universe")
    public ResponseEntity<List<TickerNameResponse>> universe() {
        return ResponseEntity.ok(stockService.getUniverse());
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<StockQuoteResponse> quote(@PathVariable String ticker) {
        return ResponseEntity.ok(stockService.getQuote(ticker));
    }
}
