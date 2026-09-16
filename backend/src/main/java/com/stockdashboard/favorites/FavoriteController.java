package com.stockdashboard.favorites;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(favoriteService.listFavorites(userId(authentication)));
    }

    @PostMapping
    public ResponseEntity<Void> add(Authentication authentication, @Valid @RequestBody AddFavoriteRequest request) {
        favoriteService.addFavorite(userId(authentication), request.ticker());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{ticker}")
    public ResponseEntity<Void> remove(Authentication authentication, @PathVariable String ticker) {
        favoriteService.removeFavorite(userId(authentication), ticker);
        return ResponseEntity.ok().build();
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
