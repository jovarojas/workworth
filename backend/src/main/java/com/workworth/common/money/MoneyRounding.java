package com.workworth.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyRounding {

    public static final int MONEY_SCALE = 2;
    public static final int RATE_SCALE = 12;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyRounding() {
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING_MODE);
    }
}
