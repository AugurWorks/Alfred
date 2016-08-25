package com.augurworks.alfred.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.augurworks.alfred.RectNetFixed;

public class BigDecimals {

    public static final int DEFAULT_PRECISION = 40;
    public static final MathContext MATH_CONTEXT = new MathContext(DEFAULT_PRECISION, RoundingMode.HALF_UP);

    private BigDecimals() {
        // utility class
    }

    /**
     * Performs the sigmoid function on an input height = 1 / (1 + exp(-alpha*depth))
     * Used internally in getOutput method. Alpha is set to 3 currently.
     *
     * @param input
     *            X
     * @return sigmoid(depth)
     */
    public static BigDecimal sigmoid(BigDecimal input) {
        //	1.0 / (1.0 + Math.exp(-3.0 * input));
        double exponential = Math.exp(-RectNetFixed.SIGMOID_ALPHA * input.doubleValue());
        return new BigDecimal(1.0 / (1.0 + exponential));
    }

}
