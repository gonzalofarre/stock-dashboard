package com.stockdashboard.favorites;

import com.stockdashboard.common.ApiException;
import com.stockdashboard.marketdata.Quote;
import com.stockdashboard.marketdata.QuoteCacheService;
import com.stockdashboard.user.User;
import com.stockdashboard.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuoteCacheService quoteCacheService;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, userRepository, quoteCacheService);
    }

    @Test
    void addFavorite_savesItUppercased() {
        when(favoriteRepository.existsByUserIdAndTicker(1L, "AAPL")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));

        favoriteService.addFavorite(1L, "aapl");

        verify(favoriteRepository).save(argThat(f -> f.getTicker().equals("AAPL")));
    }

    @Test
    void addFavorite_rejectsADuplicate() {
        when(favoriteRepository.existsByUserIdAndTicker(1L, "AAPL")).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(1L, "AAPL"))
                .isInstanceOf(ApiException.class).hasMessageContaining("Already in favorites");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void removeFavorite_deletesAnExistingOne() {
        Favorite favorite = Favorite.builder().id(9L).ticker("AAPL").build();
        when(favoriteRepository.findByUserIdAndTicker(1L, "AAPL")).thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(1L, "aapl");

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void removeFavorite_rejectsATickerThatIsNotFavorited() {
        when(favoriteRepository.findByUserIdAndTicker(1L, "AAPL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(1L, "AAPL")).isInstanceOf(ApiException.class);
    }

    @Test
    void listFavorites_mergesStoredFavoritesWithLiveQuotes() {
        Favorite favorite = Favorite.builder().ticker("AAPL").addedAt(Instant.now()).build();
        when(favoriteRepository.findByUserIdOrderByAddedAtDesc(1L)).thenReturn(List.of(favorite));
        when(quoteCacheService.getQuotes(List.of("AAPL"))).thenReturn(
                Map.of("AAPL", new Quote("AAPL", BigDecimal.valueOf(190.5), BigDecimal.valueOf(1.2), 1_000_000L, Instant.now()))
        );

        List<FavoriteResponse> result = favoriteService.listFavorites(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).quoteAvailable()).isTrue();
        assertThat(result.get(0).price()).isEqualByComparingTo("190.5");
    }

    @Test
    void listFavorites_stillReturnsATickerWhoseQuoteFailedToFetch() {
        Favorite favorite = Favorite.builder().ticker("ZZZZ").addedAt(Instant.now()).build();
        when(favoriteRepository.findByUserIdOrderByAddedAtDesc(1L)).thenReturn(List.of(favorite));
        when(quoteCacheService.getQuotes(List.of("ZZZZ"))).thenReturn(Map.of()); // provider had nothing for it

        List<FavoriteResponse> result = favoriteService.listFavorites(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).quoteAvailable()).isFalse();
        assertThat(result.get(0).price()).isNull();
    }

    @Test
    void listFavorites_returnsEmptyWithoutCallingMarketDataAtAll() {
        when(favoriteRepository.findByUserIdOrderByAddedAtDesc(1L)).thenReturn(List.of());

        List<FavoriteResponse> result = favoriteService.listFavorites(1L);

        assertThat(result).isEmpty();
        verifyNoInteractions(quoteCacheService);
    }
}
