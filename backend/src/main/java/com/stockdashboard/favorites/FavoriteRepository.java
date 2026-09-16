package com.stockdashboard.favorites;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<Favorite> findByUserIdAndTicker(Long userId, String ticker);

    boolean existsByUserIdAndTicker(Long userId, String ticker);
}
