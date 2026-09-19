package com.workworth.rewards.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * The caller's desired reading order for exactly these rewards (index 0 = shown first). Every id
 * must belong to the current user; ids not mentioned here keep their existing order untouched.
 */
public record ReorderRewardsRequest(@NotEmpty List<Long> orderedIds) {
}
