package com.workworth.rewards.persistence;

import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.identity.persistence.AppUser;
import com.workworth.rewards.domain.RewardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Getter;

@Getter
@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RewardStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_reached_context", length = 16)
    private EarningPeriod lastReachedContext;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reward() {
    }

    public Reward(AppUser user, String name, int quantity, BigDecimal price, String currencyCode, Instant createdAt) {
        this.user = user;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.currencyCode = currencyCode;
        this.status = RewardStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(String name, int quantity, BigDecimal price, Instant updatedAt) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.updatedAt = updatedAt;
    }

    public void acquire(Instant acquiredAt) {
        if (status == RewardStatus.PENDING) {
            status = RewardStatus.ACQUIRED;
            updatedAt = acquiredAt;
        }
    }

    public void updateLastReachedContext(EarningPeriod context, Instant updatedAt) {
        lastReachedContext = context;
        this.updatedAt = updatedAt;
    }
}
