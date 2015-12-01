package com.augurworks.alfred;

import java.math.BigDecimal;

/**
 * Represents and input to the Net. Has a constant output that can be set.
 *
 * @author saf
 *
 */
public class InputImpl implements Input {
    // The constant output of this Input.
    private BigDecimal value;

    /**
     * Instantiates an Input with default value of 0.
     */
    public InputImpl() {
        this.value = BigDecimal.ZERO;
    }

    /**
     * Instantiates and input with the given value.
     *
     * @param v
     *            initial value of this Input.
     */
    public InputImpl(double v) {
        this.value = BigDecimal.valueOf(v);
    }

    /**
     * Returns the value of this Input.
     *
     * @return the value of this Input.
     */
    public BigDecimal getValue() {
        return this.value;
    }

    /**
     * Returns the value of this Input
     *
     * @param code
     *            unused.
     * @return the value of this input
     */
    public BigDecimal getOutput(int code) {
        return this.value;
    }

    /**
     * Returns the value of this Input.
     *
     * @return the value of this input.
     */
    public BigDecimal getOutput() {
        return this.value;
    }

    /**
     * Sets the value of this Input.
     *
     * @param v
     *            value to set this Input to.
     */
    public void setValue(BigDecimal v) {
        this.value = v;
    }
}
