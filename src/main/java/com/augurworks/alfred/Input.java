package com.augurworks.alfred;

import java.math.BigDecimal;

/**
 * Represents any type of object that returns an output. Used for Neuron and
 * Input.
 *
 * @author saf
 *
 */
public interface Input {
    /**
     * Returns the output of this Inp.
     *
     * @param code Used in implementations for caching prior outputs.
     * @return Output of this Inp
     */
    public BigDecimal getOutput(int code);

    /**
     * Returns the output of this Inp.
     *
     * @return output of this Inp
     */
    public BigDecimal getOutput();
}
