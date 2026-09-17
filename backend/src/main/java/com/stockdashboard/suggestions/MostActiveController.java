package com.stockdashboard.suggestions;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions/most-active")
@RequiredArgsConstructor
public class MostActiveController {

    private final MostActiveService mostActiveService;

    @GetMapping
    public ResponseEntity<List<MostActiveResponse>> mostActive(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "US") Market market
    ) {
        return ResponseEntity.ok(mostActiveService.getMostActive(limit, market));
    }
}
