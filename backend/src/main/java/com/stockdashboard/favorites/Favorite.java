package com.stockdashboard.favorites;

import com.stockdashboard.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "favorite_stocks", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    void onCreate() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
        if (ticker != null) {
            ticker = ticker.toUpperCase();
        }
    }
}
