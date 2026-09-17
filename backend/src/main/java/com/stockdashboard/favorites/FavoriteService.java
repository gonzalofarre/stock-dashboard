package com.stockdashboard.favorites;

import com.stockdashboard.common.ApiException;
import com.stockdashboard.marketdata.Quote;
import com.stockdashboard.marketdata.QuoteCacheService;
import com.stockdashboard.suggestions.StockUniverse;
import com.stockdashboard.user.User;
import com.stockdashboard.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final QuoteCacheService quoteCacheService;

    @Transactional(readOnly = true)
    public List<FavoriteResponse> listFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByAddedAtDesc(userId);
        if (favorites.isEmpty()) {
            return List.of();
        }

        List<String> tickers = favorites.stream().map(Favorite::getTicker).toList();
        Map<String, Quote> quotes = quoteCacheService.getQuotes(tickers);

        return favorites.stream()
                .map(favorite -> {
                    Quote quote = quotes.get(favorite.getTicker());
                    boolean available = quote != null;
                    String name = available && quote.name() != null
                            ? quote.name()
                            : StockUniverse.COMPANY_NAMES.get(favorite.getTicker());
                    return new FavoriteResponse(
                            favorite.getTicker(),
                            name,
                            available ? quote.price() : null,
                            available ? quote.changePercent() : null,
                            available,
                            favorite.getAddedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void addFavorite(Long userId, String rawTicker) {
        String ticker = rawTicker.toUpperCase();
        if (favoriteRepository.existsByUserIdAndTicker(userId, ticker)) {
            throw ApiException.conflict("Already in favorites");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        favoriteRepository.save(Favorite.builder().user(user).ticker(ticker).build());
    }

    @Transactional
    public void removeFavorite(Long userId, String rawTicker) {
        String ticker = rawTicker.toUpperCase();
        Favorite favorite = favoriteRepository.findByUserIdAndTicker(userId, ticker)
                .orElseThrow(() -> ApiException.notFound("Not in favorites"));
        favoriteRepository.delete(favorite);
    }
}
