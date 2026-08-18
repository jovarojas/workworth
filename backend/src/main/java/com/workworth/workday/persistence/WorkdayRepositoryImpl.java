package com.workworth.workday.persistence;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

public class WorkdayRepositoryImpl implements WorkdayRepositoryCustom {

    private final EntityManager entityManager;

    public WorkdayRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockUserDate(UUID userId, LocalDate localDate) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:key, 0))")
            .setParameter("key", userId + ":" + localDate)
            .getSingleResult();
    }
}
