package com.workworth.goals.persistence;

import com.workworth.goals.domain.GoalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Getter;

@Getter
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Goal() {
    }

    public Goal(String title, BigDecimal targetAmount, String currencyCode, Instant createdAt) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.currencyCode = currencyCode;
        this.status = GoalStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(String title, BigDecimal targetAmount, Instant updatedAt) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.updatedAt = updatedAt;
    }

    public void complete(Instant closedAt) {
        status = GoalStatus.COMPLETED;
        this.closedAt = closedAt;
        updatedAt = closedAt;
    }

    public void cancel(Instant closedAt) {
        status = GoalStatus.CANCELLED;
        this.closedAt = closedAt;
        updatedAt = closedAt;
    }
}
